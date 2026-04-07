import os
import re

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    original = content
    
    # Remove standalone <p> tags with Javadoc asterisks
    content = re.sub(r' \*\s*<p>\s*\n', ' *\n', content)
    # Remove inline <p> tags
    content = re.sub(r'<p>', '', content)
    
    # Remove <ul> and </ul>
    content = re.sub(r' \*\s*<ul>\s*\n', '', content)
    content = re.sub(r' \*\s*</ul>\s*\n', '', content)
    content = re.sub(r'<ul>|</ul>', '', content)
    
    # Replace <li><b>Text</b>: with - Text:
    content = re.sub(r'<li>\s*<b>(.*?)</b>\s*:', r'- \1:', content)
    
    # Replace other <li> tags
    content = re.sub(r'<li>\s*', '- ', content)
    
    # Remove </li> tags
    content = re.sub(r'\s*</li>', '', content)
    
    # Remove <b> and </b> tags
    content = re.sub(r'<b>(.*?)</b>', r'\1', content)
    
    # Remove <br> tags
    content = re.sub(r'<br>', '', content)
    
    # Special case: 'Architectural Role: State Entity. (ADR 004)' 
    # Let's keep 'Architectural Role: State Entity.' as it's plain text once <b> is removed.
    # The prompt example showed removing the 'Architectural Role: ' prefix for one, but it also changed the text.
    # The instructions say: "remove the heavy HTML formatting... but preserve the core explanations". So just stripping HTML is enough.
    
    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated {filepath}")

for root, _, files in os.walk('d-rate-limiter-core/src/'):
    for file in files:
        if file.endswith('.java'):
            process_file(os.path.join(root, file))

