import requests

def search_hacker_news(query):
    url = 'https://hn.algolia.com/api/v1/search'
    params = {
        'query': query,
        'restrictSearchableAttributes': 'story_text'
    }
    response = requests.get(url, params=params)
    data = response.json()
    
    for hit in data['hits']:
        title = hit['title']
        url = hit['url']
        story_text = hit['story_text']
        
        if url is None:
            url = f'https://news.ycombinator.com/item?id={hit["objectID"]}'
        
        print("Title:", title)
        print("URL:", url)
        print("First Paragraph:", story_text.split('\n')[0])
        print("----------------------------------------")

# Example usage
search_hacker_news('python data science')
