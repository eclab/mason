import sys
#/bin/python2

d = {}
def clean(line):
    for i in range(11):
       line = line.replace(chr(i), " ")
    for i in range(128, 256):
        line = line.replace(chr(i), " ")
    line = line.replace("C0", "")
    return line

def cleanNumber(word):
    if word.startswith("Z") or word.startswith("C"):
        word = word[1:]
    if word.endswith("Z") or word.endswith("C"):
        word = word[:-1]
    return word
def isValidDecimal(word):
    word = cleanNumber(word)
    if word.startswith("-"):
        if len(word) == 1:
            return False
        else:
            return word[1:].isdigit()
    else:
        return word.isdigit()

def insert(word):
    print(word + str(list(map(ord, word))))
    value = raw_input(">")
    if len(value) == 0:
        value = word
    saveWrites(word, value)

def saveWrites(key, value):
    if key not in d:
        d[key] = value if len(value) != 0 else key
        # Always ignore any new values, I guess
def checkWord(word):
    if word in d:
        return d[word]
    if isValidDecimal(word) or word.isalpha():
        word = cleanNumber(word)
        saveWrites(word, word)
    elif len(word.split(".")) == 2:
        num = list(map(cleanNumber, word.split(".")))
        num = list(map(lambda x: "0" if x == "" else x, num))
        if isValidDecimal(num[0]) and num[1].isdigit():
            saveWrites(word, num[0] + "." + num[1])
        else:
            insert(word)
    else:
        insert(word)
    return d[word]
def cleanWrite(ls):
    ls = filter(lambda x: x != " " and x != "", ls)
    return " ".join(ls)

def processfile(filename):
    print("Processing " + filename)
    with open(filename, "r+") as f:
        with open("clean/"+filename, "w+") as g:
            lines = list(map(clean, f.readlines()))
            for i in lines:
                l = []
                for j in i.split():
                    l.append(checkWord(j))
                l.append("\n")
                g.write(cleanWrite(l))
    print("Finished " + filename)



def main(args):
    for i in args: processfile(i)
if __name__ == "__main__": main(sys.argv[1:])
