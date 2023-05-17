package website;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import search_module.SearchModuleInterface;
import utils.SearchResult;

@Controller
public class HomeController {
    private static final int num_of_tries = 5;
	private SearchModuleInterface search_mod = null;

    private SearchModuleInterface get_server_connection() {
		if (search_mod != null){
			try {
				search_mod.ping();
				return search_mod;
			} catch (RemoteException e) {
				// continua
			}
		}

        SearchModuleInterface smi = null;

        for (int i = 0; i < num_of_tries; i++){

            try {
                smi = (SearchModuleInterface) Naming.lookup(
                        "rmi://" + System.getProperty("cliente_rmi_ip") + "/search_mod"
                );
                break;
            } catch (MalformedURLException | RemoteException | NotBoundException e) {
                // System.out.println("\tRetrying Conection ...");
            }

            try {
                Thread.sleep(1000);  // wait 1 sec to conect
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
		search_mod = smi;
        return smi;
    }

	@GetMapping("/")
    public String bar(Model model) {
        return "navbar";
    }


	@GetMapping("/sorry")
    public String sorry(Model model) {
        return "sorry";
    }


	@GetMapping("/add-url")
    public String home(Model model) {
        return "addUrl";
    }

    @PostMapping("/add-url")
    public String processURL(@RequestParam("url") String url, Model model) {
		SearchModuleInterface smi = get_server_connection();
		if (smi == null)System.out.println("merda");
		try {
			if (smi != null &&  smi.querie_url(url)){
				model.addAttribute("successMessage", "URL processed successfully");
			}else{
				model.addAttribute("successMessage", "Failed to process URL");
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
        return "addUrl";
    }

	
	@GetMapping("/search")
    public String showSearchForm() {
        return "search";
    }

	@PostMapping("/search")
    public String search(@RequestParam("terms") String terms, Model model) {
        return "redirect:/results?q=" + terms.replace(' ', '+') + "&source=0"; // Redirect back to the search form
    }

	@GetMapping("/results")
	public String showSearchResults(@RequestParam("q") String query, @RequestParam("source") Integer source, Model model) {
		SearchModuleInterface smi = get_server_connection();

		model.addAttribute("self_query", query);

		// Process the search query
		String[] terms = query.split("\\s");
		
		List<SearchResult> results = null;
		
		if (source == 0){
			model.addAttribute("toggle_text", "Toggle Hacker News");
			model.addAttribute("source_val", 1);

			try {
				if (smi == null) return "redirect:/search";
				results = smi.search_results(Arrays.asList(terms));
			} catch (RemoteException e) {
				// e.printStackTrace();
			}

		}else{
			model.addAttribute("toggle_text", "Toggle Googol");
			model.addAttribute("source_val", 0);
			System.out.println(query);
			// results = HackerNewsSearch.searchHackerNews(query);
			results = HackerNewsSearch.searchHackerTopNews(query, smi);
			// searchHackerNews(query);
		}


		
		if (results != null){
			List<String> temp = new ArrayList<>();

			for (SearchResult res : results) {
				temp.add(res.toString());
			}
			
			model.addAttribute("items", results);
		}
	
		return "searchResults";
	}


	@GetMapping("/probe")
	public String showProbeResults(@RequestParam("q") String query, Model model) {
		SearchModuleInterface smi = get_server_connection();

		if (smi == null){
			return "redirect:/sorry";
		}
		
		List<String> results = null;
		try {
			results = smi.probe_url(query);
		} catch (RemoteException e) {
			// e.printStackTrace();
		}
		
		if (results != null){
			model.addAttribute("searchResults", results);
		}
		
		return "probeUrl";
	}


	@GetMapping("/user")
	public String user(Model model) {
		return "user";
	}

	@PostMapping("/user")
    public String user_post(@RequestParam("terms") String user, Model model) {
		SearchModuleInterface smi = get_server_connection();

		if (HackerNewsSearch.index_user_stories(user, smi)){
			model.addAttribute("successMessage", "User stories sent to index");
		}else {
			model.addAttribute("successMessage", "Failed to send to index user stories");
		}

		return "/user";

	}
}
