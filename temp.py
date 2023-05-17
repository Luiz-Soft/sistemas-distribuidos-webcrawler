# import requests

# def search_hacker_news(query):
#	 url = 'https://hn.algolia.com/api/v1/search'
#	 params = {
#		 'query': query,
#		 'restrictSearchableAttributes': 'story_text'
#	 }
#	 response = requests.get(url, params=params)
#	 data = response.json()
	
#	 for hit in data['hits']:
#		 title = hit['title']
#		 url = hit['url']
#		 story_text = hit['story_text']
		
#		 if url is None:
#			 url = f'https://news.ycombinator.com/item?id={hit["objectID"]}'
		
#		 print("Title:", title)
#		 print("URL:", url)
#		 print("First Paragraph:", story_text.split('\n')[0])
#		 print("----------------------------------------")

# # Example usage
# search_hacker_news('python data science')

import requests
from tqdm import tqdm

# Step 1: Get the top story IDs
response = requests.get('https://hacker-news.firebaseio.com/v0/topstories.json')
top_story_ids = response.json()

# Step 2: Iterate through the story IDs and retrieve story details
desired_terms = ['term1', 'term2']  # Replace with your desired terms
desired_stories = []
c = 0

for story_id in tqdm(top_story_ids):
	story_response = requests.get(f'https://hacker-news.firebaseio.com/v0/item/{story_id}.json')
	story = story_response.json()
	
	if story is None:
		c += 1
		continue

	desired_stories.append((story_id, story['title']))
	# Step 3: Check if the story's text contains desired terms
	# story_text = story.get('text', '')
	# if any(term in story_text for term in desired_terms):
	# 	desired_stories.append(story)

# Step 4: Print the desired stories
for story in desired_stories:
	print(story)
print(c)