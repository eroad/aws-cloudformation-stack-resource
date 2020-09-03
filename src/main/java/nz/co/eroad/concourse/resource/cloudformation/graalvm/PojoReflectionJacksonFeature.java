package nz.co.eroad.concourse.resource.cloudformation.graalvm;


import com.oracle.svm.core.annotate.AutomaticFeature;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import nz.co.eroad.concourse.resource.cloudformation.pojo.Metadata;
import nz.co.eroad.concourse.resource.cloudformation.pojo.OutInput;
import nz.co.eroad.concourse.resource.cloudformation.pojo.Parameter;
import nz.co.eroad.concourse.resource.cloudformation.pojo.Params;
import nz.co.eroad.concourse.resource.cloudformation.pojo.Source;
import nz.co.eroad.concourse.resource.cloudformation.pojo.Tag;
import nz.co.eroad.concourse.resource.cloudformation.pojo.Version;
import nz.co.eroad.concourse.resource.cloudformation.pojo.VersionInput;
import nz.co.eroad.concourse.resource.cloudformation.pojo.VersionMetadata;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

@AutomaticFeature
public class PojoReflectionJacksonFeature implements Feature {

  public PojoReflectionJacksonFeature() {}

  @Override
  public void beforeAnalysis(Feature.BeforeAnalysisAccess access) {
    registerPojo(Metadata.class);
    registerPojo(OutInput.class);
    registerPojo(Parameter.class);
    registerPojo(Tag.class);
    registerPojo(Params.class);
    registerPojo(Source.class);
    registerPojo(Version.class);
    registerPojo(VersionInput.class);
    registerPojo(VersionMetadata.class);


  }

  private void registerPojo(Class<?> clazz) {
    RuntimeReflection.register(clazz);
    for (Constructor<?> declaredConstructor : clazz.getDeclaredConstructors()) {
      RuntimeReflection.register(declaredConstructor);
    }
    for (Field field : clazz.getFields()) {
      RuntimeReflection.register(field);
    }

    for (Method method : clazz.getMethods()) {
      RuntimeReflection.register(method);
    }

  }


}
