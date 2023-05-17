package website;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {

		if (args.length == 0){
			System.out.println("Correct usage\n\t cliente.jar <\"ip:port\"_of_smi>");
			return;
		}

		System.setProperty("cliente_rmi_ip", args[0]);
		// System.setProperty("cliente_rmi_ip", "localhost:1097");
		HackerNewsSearch.scrape_top_stories();


		SpringApplication.run(Application.class, args);
	}

}
