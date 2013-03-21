import java.awt.HeadlessException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;


/**
 * 界面
 * @author 机械师
 * {@link http://blog.csdn.net/liuwenhan999}
 * 
 * */
public class ScreenShotView extends JFrame{

	public static void main(String[] args){
		new ScreenShotView();
	}
	public ScreenShotView() throws HeadlessException {
		super();
		init();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(500,500);
		setVisible(true); 
	}
	ScreenShot screenshot;
	JLabel label;
	MouseAdapter mClickMouseAdapter = new MouseAdapter(){
	    @Override
        public void mouseClicked(MouseEvent arg0) {
	        screenshot.setRol();
	        int x= arg0.getX();
	        int y= arg0.getY();
//	        screenshot.click(370, 399);
	        System.out.println("click x:"+x+" ,Y:"+y);
            super.mouseClicked(arg0);
        }
 
	};
	void init(){
		label=new JLabel();
		add(label);
		label.addMouseListener(mClickMouseAdapter);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		screenshot=new ScreenShot(new ScreenShot.ImageRecieveListener(){

			@Override
			public void onImageRecieved(ImageIcon icon) {
				// TODO Auto-generated method stub
				label.setIcon(icon);
			}
			
		});
		screenshot.start();
		
	}
}
