package android.app;
public class KeyguardManager
{
@Deprecated
public class KeyguardLock
{
KeyguardLock() { throw new RuntimeException("Stub!"); }
public  void disableKeyguard() { throw new RuntimeException("Stub!"); }
public  void reenableKeyguard() { throw new RuntimeException("Stub!"); }
}
public static interface OnKeyguardExitResult
{
public abstract  void onKeyguardExitResult(boolean success);
}
KeyguardManager() { throw new RuntimeException("Stub!"); }
public  android.content.Intent createConfirmDeviceCredentialIntent(java.lang.CharSequence title, java.lang.CharSequence description) { throw new RuntimeException("Stub!"); }
@java.lang.Deprecated()
public  android.app.KeyguardManager.KeyguardLock newKeyguardLock(java.lang.String tag) { throw new RuntimeException("Stub!"); }
public  boolean isKeyguardLocked() { throw new RuntimeException("Stub!"); }
public  boolean isKeyguardSecure() { throw new RuntimeException("Stub!"); }
public  boolean inKeyguardRestrictedInputMode() { throw new RuntimeException("Stub!"); }
public  boolean isDeviceLocked() { throw new RuntimeException("Stub!"); }
public  boolean isDeviceSecure() { throw new RuntimeException("Stub!"); }
@java.lang.Deprecated()
public  void exitKeyguardSecurely(android.app.KeyguardManager.OnKeyguardExitResult callback) { throw new RuntimeException("Stub!"); }
}