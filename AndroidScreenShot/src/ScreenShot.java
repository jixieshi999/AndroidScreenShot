import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.Log;
import com.android.ddmlib.RawImage;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.android.ddmlib.Log.ILogOutput;
import com.android.ddmlib.Log.LogLevel;

public class ScreenShot implements Runnable {


    JShell mJShell;
    
    IDevice mTarget = null;
    
	public interface ImageRecieveListener{
		void onImageRecieved(ImageIcon icon);
	}
	private ImageRecieveListener imagerecievelistener;
	public ScreenShot(ImageRecieveListener imagerecievelistener) {
		super();
		this.imagerecievelistener = imagerecievelistener;
	}

	private boolean run;

	
	
	public void start() {
		run = true;
	    mJShell = new JShell();
		new Thread(this).start();
	}

	public void stop() {
		run = false;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		main(null);
	}

	/**
	 * 是否旋转*/
	boolean rol = false;
	public boolean isRol() {
        return rol;
    }

    public void setRol() {
        this.rol = !rol;
    }
    IShellOutputReceiver shellR = new IShellOutputReceiver() {
        
        @Override
        public boolean isCancelled() {
            // TODO Auto-generated method stub
            return false;
        }
        
        @Override
        public void flush() {
            // TODO Auto-generated method stub
            
        }
        
        @Override
        public void addOutput(byte[] arg0, int arg1, int arg2) {
            // TODO Auto-generated method stub
            System.out.println(arg0);
            System.out.println(arg1);
            System.out.println(arg2);
        }
    };
    void click(int x,int y){
        if(null!=mTarget){
            try {
                String str=null;
                str = "adb shell sendevent /dev/input/event0 3 0 "+x;
                executeCommand(str);
                
                str = "adb shell sendevent /dev/input/event0 3 1 "+y;
                executeCommand(str);
                 
                str = "adb shell sendevent /dev/input/event0 1 330 1 ";
                executeCommand(str);
                
                str = "adb shell sendevent /dev/input/event0 0 0 0  ";
                executeCommand(str);
                 
                str = "adb shell sendevent /dev/input/event0 1 330 0 ";
                executeCommand(str);
                
                str = "adb shell sendevent /dev/input/event0 0 0 0 ";
                executeCommand(str);
                
            } catch (TimeoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (AdbCommandRejectedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ShellCommandUnresponsiveException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void executeCommand(String str) throws TimeoutException,
            AdbCommandRejectedException, ShellCommandUnresponsiveException,
            IOException {
//        mJShell.execute(str);
        mTarget.executeShellCommand(str, shellR);
    }
    
    private void main(String[] args) {
		args = new String[] { "-d", "test.png" };
		boolean device = false;
		boolean emulator = false;
		String serial = null;
		String filepath = null;
		boolean landscape = false;

		if (args.length == 0) {
			printUsageAndQuit();
		}

		// parse command line parameters.
		int index = 0;
		do {
			String argument = args[index++];

			if ("-d".equals(argument)) {
				if (emulator || serial != null) {
					printAndExit("-d conflicts with -e and -s", false /* terminate */);
				}
				device = true;
			} else if ("-e".equals(argument)) {
				if (device || serial != null) {
					printAndExit("-e conflicts with -d and -s", false /* terminate */);
				}
				emulator = true;
			} else if ("-s".equals(argument)) {
				// quick check on the next argument.
				if (index == args.length) {
					printAndExit("Missing serial number after -s", false /* terminate */);
				}

				if (device || emulator) {
					printAndExit("-s conflicts with -d and -e", false /* terminate */);
				}

				serial = args[index++];
			} else if ("-l".equals(argument)) {
				landscape = true;
			} else {
				// get the filepath and break.
				filepath = argument;

				// should not be any other device.
				if (index < args.length) {
					printAndExit("Too many arguments!", false /* terminate */);
				}
			}
		} while (index < args.length);

		if (filepath == null) {
			printUsageAndQuit();
		}

		Log.setLogOutput(new ILogOutput() {
			public void printAndPromptLog(LogLevel logLevel, String tag,
					String message) {
				System.err.println(logLevel.getStringValue() + ":" + tag + ":"
						+ message);
			}

			public void printLog(LogLevel logLevel, String tag, String message) {
				System.err.println(logLevel.getStringValue() + ":" + tag + ":"
						+ message);
			}
		});

		// init the lib
		// [try to] ensure ADB is running
		String adbLocation = System
				.getProperty("com.android.screenshot.bindir"); //$NON-NLS-1$
		if (adbLocation != null && adbLocation.length() != 0) {
			adbLocation += File.separator + "adb"; //$NON-NLS-1$
		} else {
			adbLocation = "adb"; //$NON-NLS-1$
		}

		AndroidDebugBridge.init(false /* debugger support */);

		try {
			AndroidDebugBridge bridge = AndroidDebugBridge.createBridge(
					adbLocation, true /* forceNewBridge */);

			// we can't just ask for the device list right away, as the internal
			// thread getting
			// them from ADB may not be done getting the first list.
			// Since we don't really want getDevices() to be blocking, we wait
			// here manually.
			int count = 0;
			while (bridge.hasInitialDeviceList() == false) {
				try {
					Thread.sleep(100);
					count++;
				} catch (InterruptedException e) {
					// pass
				}

				// let's not wait > 10 sec.
				if (count > 100) {
					System.err.println("Timeout getting device list!");
					return;
				}
			}

			// now get the devices
			IDevice[] devices = bridge.getDevices();

			if (devices.length == 0) {
				printAndExit("No devices found!", true /* terminate */);
			}

			IDevice target = null;

			if (emulator || device) {
				for (IDevice d : devices) {
					// this test works because emulator and device can't both be
					// true at the same
					// time.
					if (d.isEmulator() == emulator) {
						// if we already found a valid target, we print an error
						// and return.
						if (target != null) {
							if (emulator) {
								printAndExit(
										"Error: more than one emulator launched!",
										true /* terminate */);
							} else {
								printAndExit(
										"Error: more than one device connected!",
										true /* terminate */);
							}
						}
						target = d;
					}
				}
			} else if (serial != null) {
				for (IDevice d : devices) {
					if (serial.equals(d.getSerialNumber())) {
						target = d;
						break;
					}
				}
			} else {
				if (devices.length > 1) {
					printAndExit(
							"Error: more than one emulator or device available!",
							true /* terminate */);
				}
				target = devices[0];
			}
			mTarget = target;
			if (target != null) {
				try {
					System.out.println("Taking screenshot from: "
							+ target.getSerialNumber());

					// ////////////////////////机械师 20110429 21:00 modify/////////////////////////////////
					while (run) {

//						getDeviceImage(target, filepath, landscape);

					    long lonTime = System.currentTimeMillis();
	                    System.out.println("start."+lonTime);
						getDeviceImage(target, filepath, rol);
						lonTime = System.currentTimeMillis()-lonTime;
	                    System.out.println("end."+lonTime);
						try {
							Thread.currentThread().sleep(45);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					// /////////////////////////////////////////////////////////
					System.out.println("Success.");
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				printAndExit("Could not find matching device/emulator.", true /* terminate */);
			}
		} finally {
			AndroidDebugBridge.terminate();
		}
	}

	/*
	 * Grab an image from an ADB-connected device.
	 */
	private void getDeviceImage(IDevice device, String filepath,
			boolean landscape) throws IOException {
		RawImage rawImage;

		try {
			rawImage = device.getScreenshot();
		} catch (TimeoutException e) {
			printAndExit("Unable to get frame buffer: timeout", true /* terminate */);
			return;
		} catch (Exception ioe) {
			printAndExit("Unable to get frame buffer: " + ioe.getMessage(),
					true /* terminate */);
			return;
		}

		// device/adb not available?
		if (rawImage == null)
			return;

		if (landscape) {
			rawImage = rawImage.getRotated();
		}

		// convert raw data to an Image
		BufferedImage image = new BufferedImage(rawImage.width,
				rawImage.height, BufferedImage.TYPE_INT_ARGB);

		int index = 0;
		int IndexInc = rawImage.bpp >> 3;
		for (int y = 0; y < rawImage.height; y++) {
			for (int x = 0; x < rawImage.width; x++) {
				int value = rawImage.getARGB(index);
				index += IndexInc;
				image.setRGB(x, y, value);
			}
		}
		
		/////////////////////机械师 20110429 21:00 modify//////////////////////////////////
		if(imagerecievelistener!=null){
			imagerecievelistener.onImageRecieved(new ImageIcon(image));
		}
		///////////////////////////////////////////////////////
		
//		if (!ImageIO.write(image, "png", new File(filepath))) {
//			throw new IOException("Failed to find png writer");
//		}
	}

	private static void printUsageAndQuit() {
		// 80 cols marker:
		// 01234567890123456789012345678901234567890123456789012345678901234567890123456789
		System.out
				.println("Usage: screenshot2 [-d | -e | -s SERIAL] [-l] OUT_FILE");
		System.out.println("");
		System.out.println("    -d      Uses the first device found.");
		System.out.println("    -e      Uses the first emulator found.");
		System.out.println("    -s      Targets the device by serial number.");
		System.out.println("");
		System.out.println("    -l      Rotate images for landscape mode.");
		System.out.println("");

		System.exit(1);
	}

	private static void printAndExit(String message, boolean terminate) {
		System.out.println(message);
		if (terminate) {
			AndroidDebugBridge.terminate();
		}
		System.exit(1);
	}
}
