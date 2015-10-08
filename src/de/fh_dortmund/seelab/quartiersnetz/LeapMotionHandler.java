package de.fh_dortmund.seelab.quartiersnetz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import org.json.JSONObject;

import com.leapmotion.leap.CircleGesture;
import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Frame;
import com.leapmotion.leap.Gesture;
import com.leapmotion.leap.GestureList;
import com.leapmotion.leap.KeyTapGesture;
import com.leapmotion.leap.Listener;
import com.leapmotion.leap.SwipeGesture;



public class LeapMotionHandler {

	
	//Attribute
	private LeapMotionListener listener;
	private Controller leapController;
	private boolean currentBulb;
	
	//Konstruktor
	public LeapMotionHandler(){
	 	this.listener = new LeapMotionListener();

        leapController = new Controller();
        leapController.setPolicyFlags(Controller.PolicyFlag.POLICY_BACKGROUND_FRAMES);
        leapController.addListener(this.listener);
        
        currentBulb=true;
	}
	
	//Methoden
	
	public void blink(){
		for(int i=0; i<=3; i++){
			this.onKeyTap();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {

			}
		}
	}
	
	private int[] getHSBValues(){
		
		String bulb;
		if(currentBulb){
			bulb="Light2";
		}
		else{
			bulb="Light1";
		}
		
		String json="";
		int[] values=null;
		try {
			 
			URL url = new URL("http://localhost:8080/rest/items/"+bulb);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
	 
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode());
			}

			BufferedReader br = new BufferedReader(new InputStreamReader(
				(conn.getInputStream())));

			String output;
//			System.out.println("Output from Server (GET) .... \n");
			while ((output = br.readLine()) != null) {
				json=json+output;
//				System.out.println(output);
			}

			conn.disconnect();
			
			JSONObject obj = new JSONObject(json);
			String state = obj.getString("state");
			
			values= new int[3];
			String line = state.substring(0, state.indexOf(','));
			values[0] = Integer.parseInt(line);
			state = state.substring(state.indexOf(',')+1);
			
			line = state.substring(0, state.indexOf(','));
			values[1] = Integer.parseInt(line);
			state=state.substring(state.indexOf(',')+1);
			
			values[2] = Integer.parseInt(state);
	 
		} catch (MalformedURLException e) {
	 
			System.err.println("Fehler beim Lesen der Werte: Maflormed URL");
	 
		} catch (IOException e) {
	 
			System.err.println("Fehler beim Lesen der Werte: IOException");
	 
		}
		
		return values;
	}

	private void sendHSBValues(int[] values){
		
		String bulb;
		if(currentBulb){
			bulb="Light2";
		}
		else{
			bulb="Light1";
		}
		
		try{
			
			URL url = new URL("http://localhost:8080/rest/items/"+bulb);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "text/plain");

			String input = new Integer(values[0]).toString() +","+ new Integer(values[1]).toString()+","+values[2];

			OutputStream os = conn.getOutputStream();
			os.write(input.getBytes());
			os.flush();

			if (conn.getResponseCode() != HttpURLConnection.HTTP_CREATED) {
				throw new RuntimeException("Failed : HTTP error code : "
					+ conn.getResponseCode());
			}

//			BufferedReader br = new BufferedReader(new InputStreamReader(
//					(conn.getInputStream())));
//
//			String output;
//			System.out.println("Output from Server (POST) .... \n");
//			while ((output = br.readLine()) != null) {
//				System.out.println(output);
//			}

			conn.disconnect();
			
			
		} catch (MalformedURLException e) {
	 
			System.err.println("Fehler beim Setzen der Werte: Malformed URL");
	 
		} catch (IOException e) {
	 
			System.err.println("Fehler beim Setzen der Werte: IOException");
	 
		}
	}
	
	public void onKeyTap() {
		
		int[] values = getHSBValues();
		
		if(values != null){
			
			if(values[2]>0){
				values[2]=0;
			}
			else{
				values[2]=100;
			}
			
			sendHSBValues(values);
		}
			
	}
		

	public void onSwipe() {
		currentBulb=!currentBulb;
		blink();
		
	}
	

	public void onCircle(boolean clockwiseness) {
		int[] values = getHSBValues();
		
		if(clockwiseness){
			values[0]+=10;
			values[0]=values[0]%360;
		}
		else{
			if(values[0]==0){
				values[0]=350;
			}
			else{
				values[0]-=10;
			}
		}
		sendHSBValues(values);
		
	}
	
	
	//===================================
	//===    Innere Listenerklasse    ===
	//===================================
	
	private class LeapMotionListener extends Listener{
		
		private final static int CIRCLE_COUNTER_THRESHOLD = 15;
		private int circleEventCounter;
		
		@Override
		public void onConnect(Controller controller){
			controller.enableGesture(Gesture.Type.TYPE_KEY_TAP);
			//System.out.println("KeyTap Status: "+controller.isGestureEnabled(Gesture.Type.TYPE_KEY_TAP));
			controller.enableGesture(Gesture.Type.TYPE_SWIPE);
			//System.out.println("Swipe Status: "+controller.isGestureEnabled(Gesture.Type.TYPE_SWIPE));
			controller.enableGesture(Gesture.Type.TYPE_CIRCLE);
			//System.out.println("Circle Status: "+controller.isGestureEnabled(Gesture.Type.TYPE_CIRCLE));
		}
		
		public void onFrame(Controller controller){
			Frame frame = controller.frame();
			GestureList gestures = frame.gestures();
			
            for (int i = 0; i < gestures.count(); i++) {
            	System.out.println("Count of Gestures: "+gestures.count());
                Gesture gesture = gestures.get(i);
                
                switch(gesture.type()){
                	case TYPE_KEY_TAP:{
                		KeyTapGesture keyTap = new KeyTapGesture(gesture);
//                        System.err.println("Key Tap id: " + keyTap.id() + ", "
//                                + keyTap.state() + ", position: "
//                                + keyTap.position() + ", direction: "
//                                + keyTap.direction());
                        
                        onKeyTap();
                	}
                		break;
                	case TYPE_SWIPE:{
                		SwipeGesture swipe = new SwipeGesture(gesture);
//                        System.err.println("Swipe id: " + swipe.id() + ", "
//                                + swipe.state() + ", position: "
//                                + swipe.position() + ", direction: "
//                                + swipe.direction());
                        
                        onSwipe();
                	}
                		break;
                	case TYPE_CIRCLE:{
    					CircleGesture circle = new CircleGesture(gesture);
    					// Calculate clock direction using the angle between circle
    					// normal and pointable
    					boolean clockwiseness;
    					if (circle.pointable().direction().angleTo(circle.normal()) <= Math.PI / 4) {
    						// Clockwise if angle is less than 90 degrees
    						clockwiseness = true;
    					} else {
    						clockwiseness = false;
    					}
    					
    					// Calculate angle swept since last frame
    					double sweptAngle = 0;
    					if (circle.state() == com.leapmotion.leap.Gesture.State.STATE_START) {
    						// nothing to do
    					}
    					else if (circle.state() == com.leapmotion.leap.Gesture.State.STATE_UPDATE) {
    						CircleGesture previousUpdate = new CircleGesture(controller.frame(1).gesture(circle.id()));
    						sweptAngle = (circle.progress() - previousUpdate.progress()) * 2 * Math.PI;
    						
    						if (circleEventCounter <= CIRCLE_COUNTER_THRESHOLD) {
    							circleEventCounter++;
    							return;
    						} else {
    							circleEventCounter = 0;
    							onCircle(clockwiseness);
    							
//    							System.err.println("Circle id: " + circle.id() + ", "
//    									+ circle.state() + ", progress: "
//    									+ circle.progress() + ", radius: "
//    									+ circle.radius() + ", angle: "
//    									+ Math.toDegrees(sweptAngle) + ", "
//    									+ clockwiseness);
    						}
    					}
    					else if (circle.state() == com.leapmotion.leap.Gesture.State.STATE_STOP) {
    						// nothing to do
    					}
                	}	
    					break;
                	default: System.err.println("Unknown Gesture Type.");
                }
            }
			
		}
		
	}

}
