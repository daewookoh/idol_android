#!/opt/homebrew/bin/python3
# -*- coding:utf-8 -*-
#
# iOS : ./google_langtool.py ; rsync -ar ./output/ios/* ~/Documents/workspace/idol_app_ios_2/idolapp/
#
import os
import shutil
import sys
import requests
import csv
import io
import codecs
import html
from shutil import copyfile

# 원본은 https://docs.google.com/spreadsheets/d/12J5tfsL1p5PphnKwYruy7uMLStPn-0AXTP9jDUBaplw/edit#gid=0
#docurl_idol = 'https://docs.google.com/spreadsheets/d/12J5tfsL1p5PphnKwYruy7uMLStPn-0AXTP9jDUBaplw/export?format=csv&id=12J5tfsL1p5PphnKwYruy7uMLStPn-0AXTP9jDUBaplw&gid=0'
docurl_idol = 'https://docs.google.com/spreadsheets/d/12J5tfsL1p5PphnKwYruy7uMLStPn-0AXTP9jDUBaplw/export?usp=sharing&format=csv&gid=0'

# android:ios table
language_table_android = {"zh-cn":"zh-rcn", "zh-tw":"zh-rtw"}
language_table = {"zh-cn":"zh-Hans", "zh-tw":"zh-Hant", "in":"id", "pt":"pt-BR", "fa":"fa"}

OUTPUT_DIR = 'output'
TAB = ' ' * 4

if os.path.isdir(OUTPUT_DIR):
    shutil.rmtree(OUTPUT_DIR)
os.makedirs(OUTPUT_DIR)

msgids = []
locales = []
translations = {}
translatables = {}

def unicode_csv_reader(unicode_csv_data, dialect=csv.excel, **kwargs):
    # csv.py doesn't do Unicode; encode temporarily as UTF-8:
    csv_reader = csv.reader(utf_8_decoder(unicode_csv_data),
                            dialect=dialect, **kwargs)
    for row in csv_reader:
        yield row
        # decode UTF-8 back to Unicode, cell by cell:
#         yield [unicode(cell, 'utf-8') for cell in row]

def utf_8_decoder(unicode_csv_data):
    for line in unicode_csv_data:
        encoded = line.encode('utf-8')
#        print(line)
#         yield line.encode('utf-8')
        yield line

def parse(docurl):
    global locales
    response = requests.get(docurl)
    f = io.StringIO(response.content.decode('utf-8'))

    count = 0
    skips = []
    erroroccurred = False
    for line in unicode_csv_reader(f):
        msgid = line[0]
        # 번역 불필요한 것들 마크
        translatable = line[1]
        translatables[msgid] = translatable

        if msgid == u'KEY':
            if len(locales) == 0:
                locales = line[3:]
                for locale in locales:
                    translations[locale] = {}
            elif locales != line[3:]:
                print(u'공통 시트와 앱 시트의 상단 로케일 목록이 다릅니다')
                erroroccurred = True
            continue
        if msgid not in msgids:
            msgids.append(msgid)
            count += 1
        else:
            skips.append(msgid)
        localeidx = 0
        for t in line[3:]:
            if t.find(u'％s') != -1:
                erroroccurred = True
                print(t)
                print("    Error: %s: %s: contains invalid %%s" % (locales[localeidx], msgid))
                print("")
                sys.exit(1)
            translations[ locales[localeidx] ][msgid] = t.strip()
            localeidx += 1

    if erroroccurred:
        print(u'erroroccurred=%d' % erroroccurred)
        sys.exit(1)
    return (count, skips)

print(u'Start parsing...')

(count, skips) = parse(docurl_idol)
print(u'앱 시트에서 %s개 추가' % count)
if len(skips) > 0:
    print(u'앱 시트에서 중복되어 건너뜀: ' + ', '.join(skips))

print('Loading done. processing...')
for locale in locales:
    print('processing ' + locale)

    # android
    if locale == 'default':
        folder = OUTPUT_DIR + '/android/values'
    else:
        locale_android = locale
        if locale_android in language_table_android:
            locale_android = language_table_android[locale]
        folder = OUTPUT_DIR + '/android/values-' + locale_android
    os.makedirs(folder)

    # ios
    if locale == 'default':
        folder_ios = OUTPUT_DIR + '/ios/Base.lproj'
    else:
        # convert android language code to ios language code
        locale_ios = locale
        if locale_ios in language_table:
            locale_ios = language_table[locale]

        folder_ios = OUTPUT_DIR + '/ios/' + locale_ios + '.lproj'
    os.makedirs(folder_ios)

    f = codecs.open(folder + '/strings.xml', 'w', encoding='utf8')
    f_ios = codecs.open(folder_ios + '/Localizable.strings', 'w', encoding='utf-8')

    f.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n')

    total = len(msgids)
    count = 0
    for msgid in msgids:
        count = count + 1
        print("%d/%d" % (count, total), end="\r")
        # check translatable (android only)
        translatable = ''
        formatted = ''
        if translatables[msgid] == "FALSE":
            # translatable false
            translatable = ' translatable="false"'

        # check if comment
        if msgid.startswith("/**") or msgid.startswith("<!--"):
            f.write(TAB + msgid + '\n')
            f_ios.write( msgid.replace('<!--', '/*').replace('-->', '*/') + '\n')
            continue

        # * 표시된 것들은 안드는 무시, iOS는 default와 동일한 것들로
        if translations[locale][msgid] == '*':
            trans = translations['default'][msgid].replace("'", "\\'").replace("\\\\'", "\\'")
        else:
            trans = translations[locale][msgid].replace("'", "\\'").replace("\\\\'", "\\'")
            if translatable == '' or locale == 'default':
                # Multiple substitutions specified in non-positional format of string resource 경고 방지
                if "%1$" not in trans and ("%s" in trans or "%d" in trans):
                    formatted = ' formatted="false"'

                f.write( TAB + '<string name="%s"%s%s>%s</string>\n' % (msgid, translatable, formatted, trans) )

        # iOS에서 translatable==false인 경우
        if translatable != '':
            trans = translations['default'][msgid].replace("'", "\\'").replace("\\\\'", "\\'")
        # HTML escape된거를 unicode로 변환 (예:&#10084;)
        trans_ios = trans.replace("\\'", "'").replace('"', '\\"').replace('\\\\"', '\\"').replace('\n', '')
        trans_ios = html.unescape(trans_ios)
        # trans.replace("&gt;", ">").replace("&lt;", "<").replace("\\'", "'").replace('"', '\\"').replace('\\\\"', '\\"').replace('&amp;', '&')
        string_ios = ( '"%s" = "%s";\n' ) % ( msgid, trans_ios )
        f_ios.write( string_ios )

        #print("%s %s\n" % (msgid, translations['ko'][msgid]))

    f.write('</resources>')
    f.close()
    f_ios.close()

os.makedirs(OUTPUT_DIR + '/ios/en.lproj')
copyfile(OUTPUT_DIR + '/ios/Base.lproj/Localizable.strings', OUTPUT_DIR + '/ios/en.lproj/Localizable.strings')

# InfoPlist.strings 파일 처리
print(u'Start parsing...')

msgids = []
(count, skips) = parse(docurl_idol.replace("&gid=0", "&gid=1001399927"))
print(u'앱 시트에서 %s개 추가' % count)
if len(skips) > 0:
    print(u'앱 시트에서 중복되어 건너뜀: ' + ', '.join(skips))

print('Loading done. processing...')
for locale in locales:
    print('processing ' + locale)

    # ios
    if locale == 'default':
        folder_ios = OUTPUT_DIR + '/ios/Base.lproj'
    else:
        # convert android language code to ios language code
        locale_ios = locale
        if locale_ios in language_table:
            locale_ios = language_table[locale]

        folder_ios = OUTPUT_DIR + '/ios/' + locale_ios + '.lproj'

    f_ios = codecs.open(folder_ios + '/InfoPlist.strings', 'w', encoding='utf-8')

    total = len(msgids)
    count = 0
    for msgid in msgids:
        count = count + 1
        print("%d/%d" % (count, total), end="\r")

        # check if comment
        if msgid.startswith("/**") or msgid.startswith("<!--"):
            f_ios.write( msgid.replace('<!--', '/*').replace('-->', '*/') + '\n')
            continue

        # * 표시된 것들은 안드는 무시, iOS는 default와 동일한 것들로
        if translations[locale][msgid] == '*':
            trans = translations['default'][msgid].replace("'", "\\'").replace("\\\\'", "\\'")
        else:
            trans = translations[locale][msgid].replace("'", "\\'").replace("\\\\'", "\\'")

        # iOS에서 translatable==false인 경우
        if translatable != '':
            trans = translations['default'][msgid].replace("'", "\\'").replace("\\\\'", "\\'")
        # HTML escape된거를 unicode로 변환 (예:&#10084;)
        trans_ios = trans.replace("\\'", "'").replace('"', '\\"').replace('\\\\"', '\\"').replace('\n', '')
        trans_ios = html.unescape(trans_ios)
        string_ios = ( '%s = "%s";\n' ) % ( msgid, trans_ios )
        f_ios.write( string_ios )
    
    f_ios.close()
