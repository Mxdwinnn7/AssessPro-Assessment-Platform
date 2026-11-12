import java.lang.reflect.*;
public class InspectQuestionAnalytics {
  public static void main(String[] args) throws Exception {
    Class<?> c = Class.forName("QuestionAnalyticsFrame");
    System.out.println("class=" + c.getName());
    for (Constructor<?> ctor : c.getDeclaredConstructors()) {
      System.out.println("ctor=" + ctor);
    }
    for (Method m : c.getDeclaredMethods()) {
      System.out.println("method=" + m.getName() + " sig=" + m.toGenericString());
    }
    for (Field f : c.getDeclaredFields()) {
      System.out.println("field=" + f.getName() + " type=" + f.getType().getName());
    }
  }
}
