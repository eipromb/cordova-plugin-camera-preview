package com.cordovaplugincamerapreview;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.Manifest;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

public class CameraPreview extends CordovaPlugin implements CameraActivity.CameraPreviewListener {

  private final String TAG = "CameraPreview";
  private final String setOnPictureTakenHandlerAction = "setOnPictureTakenHandler";
  private final String setOnCameraReadyHandlerAction = "setOnCameraReadyHandler";
  private final String setColorEffectAction = "setColorEffect";
  private final String startCameraAction = "startCamera";
  private final String stopCameraAction = "stopCamera";
  private final String switchCameraAction = "switchCamera";
  private final String setFlashModeAction = "setFlashMode";
  private final String takePictureAction = "takePicture";
  private final String showCameraAction = "showCamera";
  private final String hideCameraAction = "hideCamera";
  private final String setFocusModeAction = "setFocusMode";
  private final String getFocusModeAction = "getFocusMode";
  private final String getSupportedFocusModesAction = "getSupportedFocusModes";

  private final String permission = Manifest.permission.CAMERA;

  private final int permissionsReqId = 0;
  private CallbackContext execCallback;
  private JSONArray execArgs;

  private CameraActivity fragment;
  private CallbackContext takePictureCallbackContext;
  private CallbackContext cameraReadyCallbackContext;
  private FrameLayout containerView;

  public CameraPreview(){
    super();
    Log.d(TAG, "Constructing");
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

    if (setOnPictureTakenHandlerAction.equals(action)){
      return setOnPictureTakenHandler(args, callbackContext);
    }
    else if (setOnCameraReadyHandlerAction.equals(action)){
      return setOnCameraReadyHandler(args, callbackContext);
    }
    else if (startCameraAction.equals(action)){
      if (cordova.hasPermission(permission)) {
        return startCamera(args, callbackContext);
      }
      else {
        execCallback = callbackContext;
        execArgs = args;
        cordova.requestPermission(this, permissionsReqId, permission);
      }
    }
    else if (takePictureAction.equals(action)){
      return takePicture(args, callbackContext);
    }
    else if (setColorEffectAction.equals(action)){
      return setColorEffect(args, callbackContext);
    }
    else if (stopCameraAction.equals(action)){
      return stopCamera(args, callbackContext);
    }
    else if (hideCameraAction.equals(action)){
      return hideCamera(args, callbackContext);
    }
    else if (showCameraAction.equals(action)){
      return showCamera(args, callbackContext);
    }
    else if (setFocusModeAction.equals(action)) {
      return setFocusMode(args, callbackContext);
    }
    else if (getFocusModeAction.equals(action)) {
      return getFocusMode(args, callbackContext);
    }
    else if (getSupportedFocusModesAction.equals(action)) {
      return getSupportedFocusModes(args, callbackContext);
    }
    else if (switchCameraAction.equals(action)){
      return switchCamera(args, callbackContext);
    } else if (setFlashModeAction.equals(action)){
      return setFlashMode(args, callbackContext);
    }

    return false;
  }

  private boolean startCamera(final JSONArray args, final CallbackContext callbackContext) {
    if(fragment != null){
      return false;
    }
    fragment = new CameraActivity();
    fragment.setEventListener(this);

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {

        try {
          DisplayMetrics metrics = cordova.getActivity().getResources().getDisplayMetrics();
          int x = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, args.getInt(0), metrics);
          int y = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, args.getInt(1), metrics);
          int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, args.getInt(2), metrics);
          int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, args.getInt(3), metrics);
          String defaultCamera = args.getString(4);
          Boolean tapToTakePicture = args.getBoolean(5);
          Boolean dragEnabled = args.getBoolean(6);
          Boolean toBack = args.getBoolean(7);

          fragment.defaultCamera = defaultCamera;
          fragment.tapToTakePicture = tapToTakePicture;
          fragment.dragEnabled = dragEnabled;
          fragment.setRect(x, y, width, height);

          //create or update the layout params for the container view
          Activity activity = cordova.getActivity();
          if(containerView == null){
            containerView = new FrameLayout(activity.getApplicationContext());
            // Look up a view id we inject to ensure there are no conflicts
						int cameraViewId = activity.getResources().getIdentifier(activity.getClass().getPackage().getName() + ":id/camera_container", null, null);
						containerView.setId(cameraViewId);
          }
          if (containerView.getParent() != webView.getView().getParent()) {
						if (containerView.getParent() != null) {
							((ViewGroup) containerView.getParent()).removeView(containerView);
						}
						FrameLayout.LayoutParams containerLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
						((ViewGroup) webView.getView().getParent()).addView(containerView, containerLayoutParams);
					}
          //display camera bellow the webview
          if(toBack){
            webView.getView().setBackgroundColor(0x00000000);
            ((ViewGroup)webView.getView()).bringToFront();
          }
          else{
            //set camera back to front
            containerView.setAlpha(Float.parseFloat(args.getString(8)));
            containerView.bringToFront();
          }



          //add the fragment to the container
          FragmentManager fragmentManager = cordova.getActivity().getFragmentManager();
          FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
          fragmentTransaction.add(containerView.getId(), fragment);
          fragmentTransaction.commit();

          callbackContext.success("good");
        }
        catch(Exception e){
          callbackContext.error(e.toString());

          e.printStackTrace();


        }
      }
    });
    return true;
  }
  private boolean takePicture(final JSONArray args, CallbackContext callbackContext) {
    if(fragment == null){
      return false;
    }
    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
    pluginResult.setKeepCallback(true);
    callbackContext.sendPluginResult(pluginResult);
    try {
      double maxWidth = args.getDouble(0);
      double maxHeight = args.getDouble(1);
      fragment.takePicture(maxWidth, maxHeight);
    }
    catch(Exception e){
      e.printStackTrace();
      return false;
    }
    return true;
  }

  public void onPictureTaken(String originalPicturePath){
    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, originalPicturePath);
    pluginResult.setKeepCallback(true);
    takePictureCallbackContext.sendPluginResult(pluginResult);
  }

  public void onCameraReady(int cameraFacing) {
    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, cameraFacing);
    pluginResult.setKeepCallback(true);
    cameraReadyCallbackContext.sendPluginResult(pluginResult);
  }

  private boolean setColorEffect(final JSONArray args, CallbackContext callbackContext) {
    if(fragment == null){
      return false;
    }

    Camera camera = fragment.getCamera();
    if (camera == null){
      return true;
    }

    Camera.Parameters params = camera.getParameters();

    try {
      String effect = args.getString(0);

      if (effect.equals("aqua")) {
        params.setColorEffect(Camera.Parameters.EFFECT_AQUA);
      } else if (effect.equals("blackboard")) {
        params.setColorEffect(Camera.Parameters.EFFECT_BLACKBOARD);
      } else if (effect.equals("mono")) {
        params.setColorEffect(Camera.Parameters.EFFECT_MONO);
      } else if (effect.equals("negative")) {
        params.setColorEffect(Camera.Parameters.EFFECT_NEGATIVE);
      } else if (effect.equals("none")) {
        params.setColorEffect(Camera.Parameters.EFFECT_NONE);
      } else if (effect.equals("posterize")) {
        params.setColorEffect(Camera.Parameters.EFFECT_POSTERIZE);
      } else if (effect.equals("sepia")) {
        params.setColorEffect(Camera.Parameters.EFFECT_SEPIA);
      } else if (effect.equals("solarize")) {
        params.setColorEffect(Camera.Parameters.EFFECT_SOLARIZE);
      } else if (effect.equals("whiteboard")) {
        params.setColorEffect(Camera.Parameters.EFFECT_WHITEBOARD);
      }

      fragment.setCameraParameters(params);
      return true;
    } catch(Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  /*
  private static final int FOCUS_AUTO = 0;
  private static final int FOCUS_CONTINUOUS = 1;
  private static final int FOCUS_MACRO = 2;
  */

    private boolean setFocusMode(final JSONArray args, CallbackContext callbackContext) {
      if (fragment == null) {
        // fragment == camera handler
        return false;
      }

      try {
        fragment.setFocusMode(args.getInt(0));
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }

      return true;
    }

  private boolean getFocusMode(final JSONArray args, CallbackContext callbackContext) {
    if (fragment == null) {
      // fragment == camera handler
      return false;
    }

    //String focusMode = fragment.getFocusMode();

    callbackContext.success(fragment.getFocusMode());

    return true;
  }

  private boolean getSupportedFocusModes(final JSONArray args, CallbackContext callbackContext) {
    if (fragment == null) {
      // fragment == camera handler
      callbackContext.error("fragment is null");
      return false;
    }

    try {
      callbackContext.success((new JSONArray(fragment.getSupportedFocusModes())).toString());
    } catch (Exception e) {
      callbackContext.error(e.toString());
      return false;
    }

    return true;
  }

  private boolean stopCamera(final JSONArray args, CallbackContext callbackContext) {
    if(fragment == null){
      return false;
    }

    FragmentManager fragmentManager = cordova.getActivity().getFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.remove(fragment);
    fragmentTransaction.commit();
    fragment = null;

    return true;
  }

  private boolean showCamera(final JSONArray args, CallbackContext callbackContext) {
    if(fragment == null){
      return false;
    }

    FragmentManager fragmentManager = cordova.getActivity().getFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.show(fragment);
    fragmentTransaction.commit();

    return true;
  }
  private boolean hideCamera(final JSONArray args, CallbackContext callbackContext) {
    if(fragment == null) {
      return false;
    }

    FragmentManager fragmentManager = cordova.getActivity().getFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.hide(fragment);
    fragmentTransaction.commit();

    return true;
  }
  private boolean switchCamera(final JSONArray args, CallbackContext callbackContext) {
    if(fragment == null){
      return false;
    }
    fragment.switchCamera();
    return true;
  }

  private boolean setFlashMode(final JSONArray args, CallbackContext callbackContext) {
    if(fragment == null){
      return false;
    }
    try {
      fragment.setFlashMode(args.getInt(0));
    }
    catch(Exception e){
      e.printStackTrace();
      return false;
    }
    return true;
  }

  private boolean setOnPictureTakenHandler(JSONArray args, CallbackContext callbackContext) {
    Log.d(TAG, "setOnPictureTakenHandler");
    takePictureCallbackContext = callbackContext;
    return true;
  }

  private boolean setOnCameraReadyHandler(JSONArray args, CallbackContext callbackContext) {
    cameraReadyCallbackContext = callbackContext;
    return true;
  }

  @Override
  public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
    for(int r:grantResults)
    {
      if(r == PackageManager.PERMISSION_DENIED)
      {
        execCallback.sendPluginResult(new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION));
        return;
      }
    }
    if (requestCode == permissionsReqId) {
      startCamera(execArgs, execCallback);
    }
  }
}
