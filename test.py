import requests

files = {'image': open('smiley.jpg', 'rb')}

resp = requests.post("https://www.datakick.org/api/items/000000000000/images",
    files=files
)
resp.raise_for_status()

print resp.status_code
print resp.content
