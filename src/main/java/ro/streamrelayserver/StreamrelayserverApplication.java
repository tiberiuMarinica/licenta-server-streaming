package ro.streamrelayserver;

import java.awt.EventQueue;
import java.io.IOException;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class StreamrelayserverApplication {

	public static void main(String[] args) throws IOException {
		ConfigurableApplicationContext ctx = new SpringApplicationBuilder(StreamrelayserverApplication.class).headless(false).run(args);
		EventQueue.invokeLater(() -> {

		    StreamRelayServer ex = ctx.getBean(StreamRelayServer.class);
		    ex.setVisible(true);
		});
		
	}

}

