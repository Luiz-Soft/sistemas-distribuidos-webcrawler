package website;


import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import utils.SearchResult;

public class HackerNewsSearch {
    public static void main(String[] args) {
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
}
