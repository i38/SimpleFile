package com.uniclau.simplefile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PermissionHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.uniclau.network.URLNetRequester;

import android.Manifest;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Base64;

import android.util.Log;

public class SimpleFilePlugin extends CordovaPlugin {


	private final String TAG="SimpleFilePlugin";

	// permissions
  private static final int PERMISSION_REQCODE_READ_FILE = 100;
  private static final int PERMISSION_REQCODE_WRITE_FILE = 101;
  private static final int PERMISSION_REQCODE_READ_TEXT = 102;
  private static final int PERMISSION_REQCODE_WRITE_TEXT = 103;
  private static final int PERMISSION_REQCODE_REMOVE = 104;
  private static final int PERMISSION_REQCODE_DOWNLOAD = 105;
  private static final int PERMISSION_REQCODE_CREATE_FOLDER = 106;
  private static final int PERMISSION_REQCODE_LIST = 107;
  private static final int PERMISSION_REQCODE_COPY = 108;

  private Context requestContext;
  private JSONArray requestArgs;
  private CallbackContext callback;

	@Override
	public void onPause(boolean multitasking) {
		Log.d(TAG, "onPause");
	    //URLNetRequester.CancelAll();
		super.onPause(multitasking);
	}

	@Override
	public void onResume(boolean multitasking) {
		Log.d(TAG, "onResume " );
		super.onResume(multitasking);
	}

  private boolean hasStoragePermissions() {
	  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return PermissionHelper.hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) &&
        PermissionHelper.hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }
    else {
	    return true;
    }
  }

  private boolean requestStoragePermissions(int requestCode) {
	  String[] permissions = new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    PermissionHelper.requestPermissions(this, requestCode, permissions);
    return true;
  }

	private void DeleteRecursive(File fileOrDirectory) {
		if (fileOrDirectory.isDirectory()) {
			for (File child : fileOrDirectory.listFiles()) {
				DeleteRecursive(child);
			}
		}

		fileOrDirectory.delete();
	}

	private String getRootPath(String type) {
		if ("external".equals(type)) {
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				return Environment.getExternalStorageDirectory().getAbsolutePath();
			} else {
				String packageName = requestContext.getPackageName();
				return "/data/data/" + packageName;
			}
		} else if ("internal".equals(type)) {
			return requestContext.getFilesDir().getAbsolutePath();
		} else if ("user".equals(type)) {
			return requestContext.getFilesDir().getAbsolutePath();
		} else if ("cache".equals(type)) {
			return requestContext.getCacheDir().getAbsolutePath();
		} else if ("tmp".equals(type)) {
			return requestContext.getCacheDir().getAbsolutePath();
		} else {
			return "";
		}
	}

	private byte[] readFile(String root, String fileName) throws Exception {
		Log.d(TAG, "start Read: " + root + "/" + fileName );
		byte[] buff;
		if ("bundle".equals(root)) {
			AssetManager assets = requestContext.getAssets();
			InputStream is = assets.open(fileName);
			ByteArrayOutputStream outstream = new ByteArrayOutputStream();
			int bytesRead;
			byte[] buf = new byte[4 * 1024]; // 4K buffer

			while ((bytesRead = is.read(buf)) != -1) {
				outstream.write(buf, 0, bytesRead);
			}
			buff = outstream.toByteArray();
		}
		else {
			String rootPath = getRootPath(root);

			File f = new File(rootPath + "/" + fileName);
			if (!f.exists()) {
				Log.d(TAG, "The file does not exist: " + fileName);
				throw new Exception("The file does not exist: " + fileName);
			}
			FileInputStream is = new FileInputStream(rootPath + "/" +fileName);
			buff = new byte[(int)f.length()];
			is.read(buff);
			is.close();
		}
		Log.d(TAG, "end Read: " + root + "/" + fileName );
		return buff;
	}

	private boolean readFile(final JSONArray params) throws Exception {
		cordova.getActivity().runOnUiThread(new Runnable() {
			public void run() {
				try {
					String root = params.getString(0);
					String fileName = params.getString(1);
					byte [] buff = readFile(root, fileName);
					String data64 = Base64.encodeToString(buff,  Base64.DEFAULT | Base64.NO_WRAP);

					callback.success(data64);
				}
				catch(Exception e) {
					callback.error(e.getMessage());
					return;
				}
			}
		});
		return true;
	}

  private boolean readText(final JSONArray params) throws Exception {
		cordova.getActivity().runOnUiThread(new Runnable() {
			public void run() {
				try {
					String root = params.getString(0);
					String fileName = params.getString(1);
          String encode = params.getString(2);
					byte [] buff = readFile(root, fileName);
					String text = new String(buff, encode);

					callback.success(text);
				}
				catch(Exception e) {
					callback.error(e.getMessage());
					return;
				}
			}
		});
		return true;
	}

	private void writeFile(String root, String fileName, byte [] data) throws Exception {
		Log.d(TAG, "start write: " + root + "/" + fileName );
		if ("bundle".equals(root)) {
			throw new Exception("The bundle file system is read only");
		}

		String rootPath = getRootPath(root);

		File f= new File(rootPath + "/" + fileName);
		if (f.exists()) {
			f.delete();
		}

		File dir = f.getParentFile();
		dir.mkdirs();

		FileOutputStream fstream;
		fstream = new FileOutputStream(rootPath + "/" + fileName);
		fstream.write(data);
		fstream.flush();
		fstream.close();
		Log.d(TAG, "end write: " + root + "/" + fileName );
	}

	private boolean writeFile(final JSONArray params) throws Exception {
		cordova.getActivity().runOnUiThread(new Runnable() {
			public void run() {
				try {
					String root = params.getString(0);
					String fileName = params.getString(1);
					String data64 = params.getString(2);
					byte [] data = Base64.decode(data64, Base64.DEFAULT);

					writeFile(root, fileName, data);

					callback.success();
				}
				catch(Exception e) {
					callback.error(e.getMessage());
				}
			}
		});
		return true;
	}


	private boolean writeText(final JSONArray params) throws Exception {

    cordova.getActivity().runOnUiThread(new Runnable() {
			public void run() {
				try {
					String root = params.getString(0);
					String fileName = params.getString(1);
					String text = params.getString(2);
          String encode = params.getString(3);

					writeFile(root, fileName, text.getBytes(encode));

					callback.success();
				}
				catch(Exception e) {
					callback.error(e.getMessage());
				}
			}
		});
		return true;
	}

	private boolean remove(final JSONArray args) throws Exception {
    cordova.getActivity().runOnUiThread(new Runnable() {
			public void run() {
				try {
					String root = args.getString(0);
					if ("bundle".equals(root)) {
						throw new Exception("The bundle file system is read only");
					}
					String rootPath = getRootPath(root);
					String fileName = args.getString(1);
					File f= new File(rootPath + "/" + fileName);
					if (f.exists()) {
						DeleteRecursive(f);
					}
					callback.success();
				}
				catch(Exception e) {
					callback.error(e.getMessage());
				}
			}
		});
		return true;
	}

	private boolean download(JSONArray params) throws Exception {

		final JSONArray args = params;
		final String root = args.getString(0);

		if ("bundle".equals(root)) {
			callback.error("The bundle file system is read only");
			return false;
		}

		String url;
		try {
			url = args.getString(1);
		}
		catch(Exception e) { return false;}

		URLNetRequester.NewRequest("", url, url, new URLNetRequester.AnswerHandler() {
			@Override
			public void OnAnswer(Object CallbackParam, byte[] Res) {

				if (Res == null) {


					callback.error("Network Error");
					return;
				}
				try {
					String rootPath = getRootPath(root);
					String fileName = args.getString(2);
					File f= new File(rootPath + "/" + fileName);
					if (f.exists()) {
						f.delete();
					}

					File dir = f.getParentFile();
					dir.mkdirs();

					FileOutputStream fstream;
					fstream = new FileOutputStream(rootPath + "/" +fileName);
					fstream.write(Res);
					fstream.flush();
					fstream.close();

					callback.success();
				} catch(Exception e) {
					callback.error(e.getMessage());
				}
			}
		});
		return true;
	}

	private boolean getUrl(JSONArray args) throws Exception {
		String root = args.getString(0);
		String fileName = args.getString(1);
		String res;
		if ("bundle".equals(root)) {
			res = "file:///android_asset/" + fileName;
		} else {
			String rootPath = getRootPath(root);
			res = "file://" + rootPath + "/" + fileName;
		}
		callback.success(res);
		return true;
	}

	private boolean createFolder(final JSONArray params) throws Exception {
		cordova.getActivity().runOnUiThread(new Runnable() {
			public void run() {
				try {
					String root = params.getString(0);
					if ("bundle".equals(root)) {
						throw new Exception("The bundle file system is read only");
					}
					String rootPath = getRootPath(root);
					String dirName = params.getString(1);
					File dir = new File(rootPath + "/" + dirName);
					dir.mkdirs();
					callback.success();
				} catch(Exception e) {
					Log.d(TAG, e.getMessage());
					callback.error(e.getMessage());
				}
			};
		});
		return true;
	}


	private JSONArray list(String root, String dirName) throws Exception {
		Log.d(TAG, "start list: " + root + "/" + dirName );
		JSONArray res = new JSONArray();
		if ("bundle".equals(root)) {
			if (".".equals(dirName)) dirName = "";
			Log.d(TAG, "list  - 1");
			String [] files = requestContext.getAssets().list(dirName);
			Log.d(TAG, "list  - 2");
			if (files.length == 0) {
				boolean isDirectory = true;
				try {
					// This function will raise an exception if it is a directory.
					Log.d(TAG, "list  - 3");
          requestContext.getAssets().open(dirName);
					Log.d(TAG, "list  - 4");
					isDirectory = false;
				} catch (Exception e) {}
				if (!isDirectory) {
					Log.d(TAG, "List error: Not a directory: " + dirName);
					throw new Exception(dirName + " is not a directory");
				}
			}

			int i;
			for (i=0; i<files.length; i++) {
				JSONObject fileObject = new JSONObject();
				fileObject.put("name", files[i]);
				fileObject.put("isFolder", true);
				try {
					Log.d(TAG, "list  - 2.1");
					String [] subFolders = requestContext.getAssets().list("".equals(dirName) ? files[i] : dirName + "/" +files[i]);
					Log.d(TAG, "list  - 2.2");
					if (subFolders.length == 0) {
						fileObject.put("isFolder", false);
					}
				} catch(Exception e) {
					fileObject.put("isFolder", false);
				}
				res.put(fileObject);
			}

		} else {
			String rootPath = getRootPath(root);
			File dir;
			if ("".equals(dirName) || ".".equals(dirName)) {
				dir = new File(rootPath);
			} else {
				dir = new File(rootPath + "/" + dirName);
			}


			Log.d(TAG, "list  - 5");
			if (!dir.exists()) {
				Log.d(TAG, "The folder does not exist: " + dirName);
				throw new Error("The folder does not exist: " + dirName);
			}

			Log.d(TAG, "list  - 6");
			if (!dir.isDirectory()) {
				Log.d(TAG, dirName + " is not a directory");
				throw new Error(dirName + " is not a directory");
			}


			Log.d(TAG, "list  - 7");
			File []childs =dir.listFiles();
			int i;
			for (i=0; i<childs.length; i++) {
				JSONObject fileObject = new JSONObject();
				fileObject.put("name", childs[i].getName());
				fileObject.put("isFolder", childs[i].isDirectory());
				res.put(fileObject);
			}
		}
		Log.d(TAG, "end list: " + root + "/" + dirName );
		return res;


	}


	private boolean list(final JSONArray params) throws Exception {
		Log.d(TAG, "start list (main thread): " );
		cordova.getActivity().runOnUiThread(new Runnable() {
			public void run() {
				try {

					String root = params.getString(0);
					String dirName = params.getString(1);
					JSONArray res = list(root, dirName);

					callback.success(res);
					Log.d(TAG, "end list (main thread)");
				}
				catch(Exception e) {
					callback.error(e.getMessage());
					Log.d(TAG, e.getMessage());
				}
			}
		});
		return true;
	}

	private void copy(String rootFrom, String fileFrom,String rootTo,String fileTo) throws Exception {
		Boolean isDir=false;
		JSONArray l = null;
		try {
			l=list(rootFrom, fileFrom);
			isDir = true;
		} catch (Exception E){};
		if (!isDir) {
			byte [] data = readFile(rootFrom, fileFrom);
			writeFile(rootTo,fileTo, data);
			return;
		}
		int i;
		for (i=0; i<l.length(); i++) {
			String childName = l.getJSONObject(i).getString("name");
			isDir = l.getJSONObject(i).getBoolean("isFolder");

			String newFrom = fileFrom;
			if (! "".equals(newFrom)) {
				newFrom +=  "/";
			}
			newFrom += childName;

			String newTo = fileTo;
			if (! "".equals(newTo)) {
				newTo +=  "/";
			}
			newTo += childName;

			if (isDir) {
				copy(rootFrom, newFrom, rootTo, newTo);
			} else {
				byte [] data = readFile(rootFrom, newFrom);
				writeFile(rootTo,newTo, data);
			}
		}
	}

	private boolean copy(final JSONArray params) throws Exception {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				try {

					String rootFrom = params.getString(0);
					String fileFrom = params.getString(1);
					String rootTo = params.getString(2);
					String fileTo = params.getString(3);
					copy(rootFrom, fileFrom, rootTo, fileTo);

					callback.success();
				}
				catch(Exception e) {
					Log.d(TAG, "ERROR copy: " + e.getMessage());
					callback.error(e.getMessage());
				}
			}
		});
		return true;
	}


	@Override
	public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

		try {
		  //risk: use member var for permission callback will break multi-thread call
      this.requestContext = this.cordova.getActivity();
      this.callback = callbackContext;
      this.requestArgs = args;

			if ("read".equals(action)) {
			  if (!hasStoragePermissions())
			    return requestStoragePermissions(PERMISSION_REQCODE_READ_FILE);
			  else
				  return readFile(args);
			}
			else if ("write".equals(action)) {
        if (!hasStoragePermissions())
          return requestStoragePermissions(PERMISSION_REQCODE_WRITE_FILE);
        else
				  return writeFile(args);
			}
			else if ("readText".equals(action)) {
        if (!hasStoragePermissions())
          return requestStoragePermissions(PERMISSION_REQCODE_READ_TEXT);
        else
  				return readText(args);
			}
			else if ("writeText".equals(action)) {
        if (!hasStoragePermissions())
          return requestStoragePermissions(PERMISSION_REQCODE_WRITE_TEXT);
        else
  				return writeText(args);
			}
			else if ("remove".equals(action)) {
        if (!hasStoragePermissions())
          return requestStoragePermissions(PERMISSION_REQCODE_REMOVE);
        else
  				return remove(args);
			}
			else if ("download".equals(action)) {
        if (!hasStoragePermissions())
          return requestStoragePermissions(PERMISSION_REQCODE_DOWNLOAD);
        else
  				return download(args);
			}
			else if ("getUrl".equals(action)) {
				return getUrl(args);
			}
			else if ("createFolder".equals(action)) {
        if (!hasStoragePermissions())
          return requestStoragePermissions(PERMISSION_REQCODE_CREATE_FOLDER);
        else
  				return createFolder(args);
			}
			else if ("list".equals(action)) {
        if (!hasStoragePermissions())
          return requestStoragePermissions(PERMISSION_REQCODE_LIST);
        else
  				return list(args);
			}
			else if ("copy".equals(action)) {
        if (!hasStoragePermissions())
          return requestStoragePermissions(PERMISSION_REQCODE_COPY);
        else
  				return copy(args);
			}
			return false;
		} catch(Exception e) {
			System.err.println("Exception: " + e.getMessage());
			callbackContext.error(e.getMessage());
			return false;
		}
	}

  public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
    for (int r : grantResults) {
      if (r == PackageManager.PERMISSION_DENIED) {
        this.callback.error("Please allow access and try again.");
        return;
      }
    }

    try {
      // now call the originally requested actions
      if (requestCode == PERMISSION_REQCODE_READ_FILE) {
        readFile(requestArgs);
      } else if (requestCode == PERMISSION_REQCODE_WRITE_FILE) {
        writeFile(requestArgs);
      } else if (requestCode == PERMISSION_REQCODE_READ_TEXT) {
        readText(requestArgs);
      } else if (requestCode == PERMISSION_REQCODE_WRITE_TEXT) {
        writeText(requestArgs);
      } else if (requestCode == PERMISSION_REQCODE_REMOVE) {
        remove(requestArgs);
      } else if (requestCode == PERMISSION_REQCODE_DOWNLOAD) {
        download(requestArgs);
      } else if (requestCode == PERMISSION_REQCODE_CREATE_FOLDER) {
        createFolder(requestArgs);
      } else if (requestCode == PERMISSION_REQCODE_LIST) {
        list(requestArgs);
      } else if (requestCode == PERMISSION_REQCODE_COPY) {
        copy(requestArgs);
      }

    } catch(Exception e) {
      System.err.println("Exception: " + e.getMessage());
      callback.error(e.getMessage());
    }
  }
}
