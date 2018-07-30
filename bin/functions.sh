#!/bin/bash

retries=10

is_stack_completed() {
  status="$(echo "$1" | jq  -c -r '.StackStatus')"
  exit_code=1
  case "$status" in
    CREATE_COMPLETE|UPDATE_COMPLETE|DELETE_COMPLETE) exit_code=0 ;;
  esac
  return "$exit_code"
}

is_stack_recently_deleted() {
  status="$(echo "$1" | jq  -c -r '.StackStatus')"
  exit_code=1
  case "$status" in
    DELETE_COMPLETE) exit_code=0 ;;
  esac
  return "$exit_code"
}

is_stack_not_exist() {
    echo "$1" | grep -Eq "Stack with id $2 does not exist"
    return "$?"
}

is_stack_rolled_back() {
  status="$(echo "$1" | jq -c -r '.StackStatus')"
  exit_code=1
  case "$status" in
    ROLLBACK_COMPLETE|UPDATE_ROLLBACK_COMPLETE) exit_code=0 ;;
  esac
  return "$exit_code"
}

is_stack_errored() {
  status="$(echo "$1" | jq -c -r '.StackStatus')"
  exit_code=1
  case "$status" in
    CREATE_FAILED|ROLLBACK_FAILED|DELETE_FAILED|UPDATE_ROLLBACK_FAILED) exit_code=0 ;;
  esac
  return "$exit_code"
}

update_in_progress() {
  status="$(echo "$1" | jq -c -r '.StackStatus')"
  exit_code=1
  case "$status" in
    CREATE_IN_PROGRESS|DELETE_IN_PROGRESS|REVIEW_IN_PROGRESS|ROLLBACK_IN_PROGRESS|UPDATE_COMPLETE_CLEANUP_IN_PROGRESS|UPDATE_IN_PROGRESS|UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS|UPDATE_ROLLBACK_IN_PROGRESS) exit_code=0 ;;
  esac
  return "$exit_code"
}


load_stack() {
  stacks=$(aws_with_retry --region "$1" cloudformation describe-stacks --stack-name="$2")
  status="$?"
  if [ "$status" -ne 0 ]; then
    echo "$stacks"
    return "$status"
  fi
  echo "$stacks" | jq '.Stacks[0]'
}

aws_with_retry(){
    timeout=1
    for i in $(seq "$retries"); do
        reason="$(aws "$@" 2>&1)"
        status=$?
        if [ "$status" -eq 0 ] || ! echo "$reason" | grep -q 'Rate exceeded' ; then
             echo "$reason"
             return "$status"
        fi
        timeout=$(bc <<< "scale=4; $timeout * (1.5 + $RANDOM / 32767)")
        sleep "$timeout"
    done
    echo "$reason"
    return "$status"
}

awaitComplete(){
    for i in $(seq 20); do
        output="$(load_stack "$1" "$2")"
        status="$?"
        if [ "$status" -ne 0 ]; then
            echo "$output"
            return "$status"
        elif is_stack_rolled_back "$output"; then
            echo "$output"
            return 35
        elif is_stack_errored "$output" ; then
            echo "$output"
            return 45
        elif is_stack_completed "$output"; then
            echo "$output"
            return 0
        fi
        sleep 20
    done
    echo "Timed out waiting for deploy completion!"
    return 255
}

awaitStart(){
    for i in $(seq 20); do
        output="$(load_stack "$1" "$2")"
        status="$?"
        if [ "$status" -ne 0 ]; then
            if is_stack_not_exist "$output" "$2" ; then
                echo "Stack $2 does not exist"
                return 0
            else
                echo "$output"
                return "$status"
            fi
        elif ! update_in_progress "$output" ; then
            echo "$output"
            return 0
        fi
        sleep 20
    done
    echo "Timed out waiting for deploy start!"
    return 255
}

showErrors(){
    events=$(aws_with_retry --region "$1" cloudformation describe-stack-events --stack-name "$2")
    status="$?"
    if [ "$status" -eq 0 ]; then
        echo "$events" | jq --argjson from_before "$3" '.StackEvents[] | select(.ResourceStatus | contains("FAILED")) | select(.Timestamp > ($from_before - 5 | todate))'
    else
        echo "$events"
    fi
    return "$status"
}

