package indexstoragebarrels;
class SearchResult {
    private String title;
    private String url;
    private String citation;

    public SearchResult(String title, String url, String citation) {
        this.title = title;
        this.url = url;
        this.citation = citation;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getCitation() {
        return citation;
    }

    @Override
    public String toString() {
        return "Title: " + title + "\nURL: " + url + "\nCitation: " + citation + "\n";
    }
}