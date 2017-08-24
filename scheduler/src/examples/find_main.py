import re


def find_main(path):
    file = open(path, 'r')
    # print("current file: " + file.name)
    try:
        cont = file.read()
    except:
        file.close()
        return False
    file.close()
    pattern = r'public static void main(.*)'
    match = re.search(pattern, cont)
    if match:
        return True
    else:
        return False