package android.webkit;
public class WebStorage
{
@java.lang.Deprecated()
public static interface QuotaUpdater
{
public abstract  void updateQuota(long newQuota);
}
public static class Origin
{
protected  Origin(java.lang.String origin, long quota, long usage) { throw new RuntimeException("Stub!"); }
public  java.lang.String getOrigin() { throw new RuntimeException("Stub!"); }
public  long getQuota() { throw new RuntimeException("Stub!"); }
public  long getUsage() { throw new RuntimeException("Stub!"); }
}
public  WebStorage() { throw new RuntimeException("Stub!"); }
public  void getOrigins(android.webkit.ValueCallback<java.util.Map> callback) { throw new RuntimeException("Stub!"); }
public  void getUsageForOrigin(java.lang.String origin, android.webkit.ValueCallback<java.lang.Long> callback) { throw new RuntimeException("Stub!"); }
public  void getQuotaForOrigin(java.lang.String origin, android.webkit.ValueCallback<java.lang.Long> callback) { throw new RuntimeException("Stub!"); }
@java.lang.Deprecated()
public  void setQuotaForOrigin(java.lang.String origin, long quota) { throw new RuntimeException("Stub!"); }
public  void deleteOrigin(java.lang.String origin) { throw new RuntimeException("Stub!"); }
public  void deleteAllData() { throw new RuntimeException("Stub!"); }
public static  android.webkit.WebStorage getInstance() { throw new RuntimeException("Stub!"); }
}