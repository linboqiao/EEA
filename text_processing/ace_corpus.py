import os
import xml.etree.ElementTree as ET

directory = "/shared/data/xren7/relation_extraction_data/ACE/ACE05EN/source"
for filename in os.listdir(directory):
    if filename.endswith(".sgm"):
        print(filename)
        xml_path = os.path.join(directory, filename)
        tree = ET.parse(xml_path)
        root = tree.getroot()
        text = ' '.join(list(root.find('BODY').find('TEXT').itertext()))
        out_filename = filename.split('.')[0] + '.txt'
        with open("output/" + out_filename, 'w', encoding='utf-8') as out_f:
            out_f.write(text.strip())

