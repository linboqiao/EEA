with open("data/frameNetVN.txt", encoding='utf-8') as framenet_f:
    trigger_candidates = []
    while True:
        line1 = framenet_f.readline()
        line2 = framenet_f.readline()
        if not line2:
            break
        words = line2.strip()
        if words:
            for w in words.split():
                trigger_candidates.append(w)

with open("data/triggers.txt", 'w', encoding='utf-8') as out_f:
    for w in trigger_candidates:
        out_f.write(w + '\n')

