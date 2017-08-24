# encoding=utf-8

import os
import difflib
import re
from sys import argv

import find_main


def main():
    file_name_1 = 'scheduler/scheduler/src/examples'
    # file_name_2 = 'before'
    file_name_2 = None

    if len(argv) == 3:
        file_name_1 = argv[1]
        file_name_2 = argv[2]
    elif len(argv) == 2:
        file_name_1 = argv[1]
        file_name_2 = None

    dict1 = dict()
    dict2 = dict()
    files1 = os.walk(file_name_1)
    files2 = os.walk(file_name_2)

    dirs1 = set()
    dirs2 = set()

    main_classes = set()

    for parent, dirs, files in files1:
        dict1[parent] = [dirs, files]
        result, number = re.subn('^' + file_name_1, '.', parent)
        dirs1.add(result)

        # 寻找主函数
        for file in files:
            old_path = parent + '\\' + file
            if find_main.find_main(old_path):
                if file_name_2 is not None:
                    pat = '^' + file_name_1 + '\\\\'
                    temp, num = re.subn(pat, '', old_path)
                else:
                    temp = old_path
                    num = 0
                pat = '\\\\'
                temp, num = re.subn(pat, '.', temp)
                package_path, num = re.subn('\\.java', '', temp)
                main_classes.add(package_path)

    if file_name_2 is None:
        print('The main function of the class：')
        if len(main_classes) > 0:
            for cl in main_classes:
                print('\t' + cl)
        else:
            print('The main function was not found')
        return

    for parent, dirs, files in files2:
        dict2[parent] = [dirs, files]
        result, number = re.subn('^' + file_name_2, '.', parent)
        dirs2.add(result)

    # print(dirs1)
    # print(dirs2)
    same, new_dirs, deleted_dirs = compare_set(dirs1, dirs2)
    # print(same, dirs1_new, dirs2_new)
    # new_files, deleted_files, changed_files
    new_files = set()
    deleted_files = set()
    changed_files = dict()
    for dir in same:
        dir1, num1 = re.subn(r'^\.', file_name_1, dir)
        dir2, num2 = re.subn(r'^\.', file_name_2, dir)
        # print(dir1, dir2)
        file_add, file_delete, diff_files = compare_dir(dict1[dir1], dict2[dir2], dir1, dir2)
        for file in file_add:
            new_files.add(file)
        for file in file_delete:
            deleted_files.add(file)
        for file in diff_files:
            changed_files[file] = diff_files[file]

    print('new dirs:')
    for d in new_dirs:
        print('\t' + d)
    print('deleted dirs:')
    for d in deleted_dirs:
        print('\t' + d)
    print('new files:')
    for f in new_files:
        print('\t' + f)
    print('deleted files:')
    for f in deleted_files:
        print('\t' + f)
    print('changed files:')
    for f in changed_files:
        print('\t' + f)
    if (len(main_classes) > 0):
        print('Main class:')
        for cl in main_classes:
            print('\t' + cl)
    else:
        print('The main function was not found')

    print('changed files detail:')
    for f in changed_files:
        print('\n' + f)
        rows = changed_files[f]
        for row in rows:
            print('\t' + row)


def compare_dir(dir1, dir2, dir1_path, dir2_path):
    difference = dict()

    dir1_set = set()
    dir2_set = set()

    for file in dir1[1]:
        dir1_set.add(file)
    for file in dir2[1]:
        dir2_set.add(file)

    same = dir1_set & dir2_set
    dir1_new = dir1_set - dir2_set
    dir2_new = dir2_set - dir1_set

    for filename in same:
        # print(filename + ":")
        dir1_full_path = dir1_path + '\\' + filename
        dir2_full_path = dir2_path + '\\' + filename
        fin1 = open(dir1_full_path, 'r')
        fin2 = open(dir2_full_path, 'r')
        str1 = list()
        str2 = list()
        while 1:
            line = fin1.readline()
            if not line:
                break
            out, number = re.subn(r'\n$', '', line)
            str1.append(out)
        while 1:
            line = fin2.readline()
            if not line:
                break
            out, number = re.subn(r'\n$', '', line)
            str2.append(out)
        fin1.close()
        fin2.close()

        # print(str1, str2)
        result = difflib.Differ().compare(str1, str2)
        result_list = list(result)
        is_different = False
        for r in result_list:
            pattern = r'^[\+\-!]'
            match = re.search(pattern, r)
            if match:
                is_different = True
                break
        if is_different:
            difference[dir1_full_path] = result_list

    return dir1_new, dir2_new, difference


def compare_set(set1, set2):
    set1_new = set1 - set2
    set2_new = set2 - set1
    same = set1 & set2
    return same, set1_new, set2_new

if __name__ == '__main__':
    main()