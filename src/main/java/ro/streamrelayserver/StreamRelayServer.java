package ro.streamrelayserver;

import java.awt.FlowLayout;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UTFDataFormatException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

@Component
public class StreamRelayServer extends JFrame {

	private RabbitTemplate rabbitTemplate;
	
	private static final long serialVersionUID = 1L;

	private JLabel myLabel;
	
	private volatile String sharedImageAsString = "";
	
	private AtomicInteger numberOfClients = new AtomicInteger(0);
	private AtomicBoolean isStreamOn = new AtomicBoolean(false);

	public StreamRelayServer(RabbitTemplate rabbitTemplate) throws IOException {
		this.rabbitTemplate = rabbitTemplate;
		
		myLabel = new JLabel(
				new ImageIcon("C:\\Users\\tiber\\Pictures\\RKyaEDwp8J7JKeZWQPuOVWvkUjGQfpCx_cover_580.jpg"));

		add(myLabel);

		// Setting Frame width and height
		setSize(608, 480);

		// Setting the title of Frame
		setTitle("This is my First AWT example");

		// Setting the layout for the Frame
		setLayout(new FlowLayout());

		/*
		 * By default frame is not visible so we are setting the visibility to true to
		 * make it visible.
		 */
		setVisible(true);

		ExecutorService es = null;
		try {
			es = Executors.newCachedThreadPool();

			es.submit(() -> handleStreamFromRaspberryPi());

			es.submit(() -> sendRabbitMQMessageCommands());
			
			listenForAndroidClientConnectionAndDispatchToThread(es);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (es != null) {
				es.shutdown();
			}
		}

	}

	private void listenForAndroidClientConnectionAndDispatchToThread(ExecutorService es) throws IOException {
		ServerSocket ss = new ServerSocket(8001);
		
		// running infinite loop for getting
		// client request
		while (true) {
			Socket s = null;

			try {
				// socket object to receive incoming client requests
				s = ss.accept();

				System.out.println("A new client is connected : " + s);

				// obtaining input and out streams
				DataOutputStream dos = new DataOutputStream(s.getOutputStream());

				System.out.println("Assigning new thread for this client");

				// Invoking the start() method
				es.submit(() -> handleClientRequestFromAndroid(dos));

			} catch (Exception e) {
				s.close();
				ss.close();
				e.printStackTrace();
			}
		}
	}

	private void sendRabbitMQMessageCommands() {
		while(true) {
			try {
				
				System.out.println("Number of clients: " + numberOfClients.get());
				
				if(numberOfClients.get() > 0 && !isStreamOn.get()){
					String commandJson = new Gson().toJson(new Command("START_LIVE_STREAM"));
					
					rabbitTemplate.convertAndSend("comenzi", "comenzi", commandJson);
					
					isStreamOn.set(true);
				}
				
				if(numberOfClients.get() == 0 && isStreamOn.get()) {
					String commandJson = new Gson().toJson(new Command("STOP_LIVE_STREAM"));
					
					rabbitTemplate.convertAndSend("comenzi", "comenzi", commandJson);
					
					isStreamOn.set(false);
				}
				
				Thread.sleep(2000);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	private void handleClientRequestFromAndroid(DataOutputStream dos) {
		System.out.println("Assigned thread " + Thread.currentThread().getName());
		numberOfClients.incrementAndGet();
		
		try {
			while(true) {
				try{
					dos.writeUTF(sharedImageAsString); 
				}catch(UTFDataFormatException exe) {
					exe.printStackTrace();
				}
				Thread.sleep(100);
			}
		} catch(SocketException se) {
			numberOfClients.decrementAndGet();
			se.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	private void handleStreamFromRaspberryPi() {

		ServerSocket welcomeSocket = null;
		try {
			welcomeSocket = new ServerSocket(8000);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {

			while (true) {
				Socket connectionSocket = welcomeSocket.accept();

				Reader r = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));

				int i = 0;
				int intch;

				char[] buffer = new char[2000000];

				char[] charArr;

				while ((intch = r.read()) != -1) {
					char c = (char) intch;

					if (c == '*') {

						charArr = new char[i];

						for (int j = 0; j < i; j++) {
							charArr[j] = buffer[j];
						}

						sharedImageAsString = new String(charArr);

						myLabel.setIcon(new ImageIcon(Base64.getDecoder().decode(sharedImageAsString)));

						buffer = new char[2000000];
						i = 0;
					} else {
						if (!Character.isLetter(c) && !Character.isDigit(c) && c != '+' && c != '/' && c != '=') {
							System.out.println("Current character is invalid! " + c);
							continue;
						}

						buffer[i] = c;
						i++;
					}

				}
			}

		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} finally {
			try {
				welcomeSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
