package website;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {

		if (args.length == 0){
			System.out.println("Correct usage\n\t cliente.jar <\"ip:port\"_of_cliente>");
			return;
		}

		System.setProperty("cliente_rmi_ip", args[0]);

		SpringApplication.run(Application.class, args);
	}

}
