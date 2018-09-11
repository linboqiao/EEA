train = []
with open("FileList", "r") as f:
    for line in f.readlines():
        args = line.split("\t")
        str1 = "bc/timex2norm/"
        train.append(str1 + args[0])

print(train)
i = 0
for line in train:
    if i <= 30:
        with open("train.txt","a+") as f2:
            f2.write(line + "\n")
    elif i <= 40:
        with open("valid.txt","a+") as f3:
            f3.write(line + "\n")
    else:
        with open("test.txt","a+") as f4:
            f4.write(line + "\n")
    i=i+1

