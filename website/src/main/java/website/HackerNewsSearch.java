package website;


import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import search_module.SearchModuleInterface;
import utils.SearchResult;

public class HackerNewsSearch {
	private static HashMap<String, HashSet<Integer>> index = new HashMap<>();
	private static HashSet<Integer> ids_seen = new HashSet<>();

	public static String get_json(String url) throws IOException, InterruptedException{
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(java.net.URI.create(url))
			.build();
		
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		String responseBody = response.body();

		return responseBody;
	}

	private static void index_page(int id, String string) {
		for (String word : string.split("\\s")) {
			HashSet<Integer> temp = index.get(word);

			if(temp == null){
				temp = new HashSet<>();
				index.put(word, temp);
			};
			temp.add(id);
		}
		System.out.println("Scraped "+id);
	}

    public static void scrape_top_stories() {
		Runnable assignDownloadersRunnable = () -> {
			try {
				JSONArray data = new JSONArray(get_json("https://hacker-news.firebaseio.com/v0/topstories.json"));
				
				for (int i = 0; i < data.length(); i++) {
					int id = data.getInt(i);
					
					if (ids_seen.contains(id)) continue;
					ids_seen.add(id);
					
					try {
						JSONObject page = new JSONObject(get_json("https://hacker-news.firebaseio.com/v0/item/"+id+".json"));
						if (page.getString("type").equals("story") || !page.isNull("text"))
							index_page(id, page.getString("text"));
					} catch (IOException | InterruptedException | org.json.JSONException e) {
						continue;
					}
					
				}

			} catch (IOException | InterruptedException e) {
				// pass
			}
			System.out.println("FINISHED");
        };

        Thread assignDownloadersThread = new Thread(assignDownloadersRunnable);
        assignDownloadersThread.start();
	}

	private static Set<Integer> search(List<String> terms) {
        System.out.println("Searching for " + String.join(", ", terms));

        Set<Integer> commonUrls = new HashSet<>();

        boolean firstTerm = true;
        for (String term : terms) {
            HashSet<Integer> urls = index.get(term);

            if (urls == null) {
                return commonUrls; // If any term is not found, return an empty list
            }

            if (firstTerm) {
                commonUrls.addAll(urls);
                firstTerm = false;
            } else {
                commonUrls.retainAll(urls);
            }
        }

		return commonUrls;
	}


	public static void main(String[] args) {
		// nnurmanov
        String query = "python data science";
        searchHackerNews(query);
    }

    public static List<SearchResult> searchHackerNews(String query) {
        String url = "https://hn.algolia.com/api/v1/search";
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String params = "query=" + encodedQuery + "&restrictSearchableAttributes=story_text";
        String fullUrl = url + "?" + params;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(fullUrl))
                .build();

		List<SearchResult> resp = new ArrayList<>();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            // Parse JSON response
            JSONObject data = new JSONObject(responseBody);
            JSONArray hits = data.getJSONArray("hits");

            for (int i = 0; i < hits.length(); i++) {
                JSONObject hit = hits.getJSONObject(i);

                String title = hit.isNull("title") ? null : hit.getString("title");
                url = hit.isNull("url") ? null : hit.getString("url");
                String storyText = hit.isNull("story_text")? null : hit.getString("story_text");

                if (url == null || url.equals("null")) {
                    url = "https://news.ycombinator.com/item?id=" + hit.getString("objectID");
                }

				// Extract first <p> tag
                String firstParagraph = extractFirstParagraph(storyText);

				resp.add(new SearchResult(title, url, firstParagraph));

                // System.out.println("Title: " + title);
                // System.out.println("URL: " + url);
				// if (storyText != null) System.out.println("First Paragraph: " + storyText.split("\n")[0]);
                
				// System.out.println("----------------------------------------");
            }

			return resp;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

		return null;
    }

	public static List<SearchResult> searchHackerTopNews(String query, SearchModuleInterface smi) {
		scrape_top_stories(); // upadte
		Set<Integer> data = search(Arrays.asList(query.split("\\s")));
		
		List<SearchResult> resp = new ArrayList<>();
		
		for (Integer id : data) {
			try {
				JSONObject page = new JSONObject(get_json("https://hacker-news.firebaseio.com/v0/item/"+id+".json"));

				try {
					smi.querie_url("https://news.ycombinator.com/item?id="+id);
				} catch (RemoteException e) {
					System.out.println("Error remote");
				}

				String title = page.isNull("title") ? null : page.getString("title");
                String url = page.isNull("url") ? null : page.getString("url");
                String storyText = page.isNull("text")? null : page.getString("text");

				if (url == null || url.equals("null")) {
                    url = "https://news.ycombinator.com/item?id=" + id;
                }

				String firstParagraph = extractFirstParagraph(storyText);

				resp.add(new SearchResult(title, url, firstParagraph));

			} catch (IOException | InterruptedException e) {
				continue;
			}
		}

		return resp;
	}

	public static String extractFirstParagraph(String storyText) {
        // Find the start and end index of the first <p> tag
        int startIndex = storyText.indexOf("<");
        // int endIndex = storyText.indexOf("</p>", startIndex);

        if (startIndex != -1) {
            // Extract the content between the <p> tags
            String firstParagraph = storyText.substring(0, startIndex);
            return firstParagraph;
        }

        return storyText;
    }


	public static boolean index_user_stories(String username, SearchModuleInterface smi) {
		JSONObject data;
		try {
			data = new JSONObject(get_json("https://hacker-news.firebaseio.com/v0/user/"+username+".json"));
		} catch (JSONException | IOException | InterruptedException e) {
			return false;
		}

		Runnable assignDownloadersRunnable = () -> {
			JSONArray subm = data.getJSONArray("submitted");
			// System.out.println("https://hacker-news.firebaseio.com/v0/user/"+username+".json");
			if (subm == null) return;

			for (int i = 0; i < subm.length(); i++) {
				int id = subm.getInt(i);
				
				try {
					JSONObject page = new JSONObject(get_json("https://hacker-news.firebaseio.com/v0/item/"+id+".json"));
					// System.out.println(page.getString("type"));
					if (page.getString("type").equals("story")){
						smi.querie_url("https://news.ycombinator.com/item?id="+id);
					}
				
				} catch (IOException | InterruptedException | org.json.JSONException e) {
					continue;
				}
			}
        };

        Thread assignDownloadersThread = new Thread(assignDownloadersRunnable);
        assignDownloadersThread.start();
		return true;
	}

}
