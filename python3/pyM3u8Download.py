import os
import queue
import threading
import time

# 下载最大重试次数
repeatMax = 5
repeatWaitTime = 30
# header = ''

# 正则模块校验
try:
    import re
except:
    os.system('sudo pip3 install re')
    import re

# requests模块校验
try:
    import requests
except:
    os.system('sudo pip3 install requests')
    import requests

try:
    import urllib.parse
except:
    os.system('sudo pip3 install urllib')
    import urllib.parse

# crypto模块校验
try:
    from Crypto.Cipher import AES
except:
    os.system('sudo pip3 install pycryptodome')
    from Crypto.Cipher import AES

queueLock = threading.Lock()
workQueue = queue.Queue()


def getSysArgv(keyReplace, safeKeys):
    inputDic = {}
    import sys
    l = sys.argv
    if l[1:] == [] or '-h' == l[1] or '-help' == l[1]:
        pass
    else:
        if '://' in l[1]:
            l = [l[0]] + ['-u'] + l[1:]
        for item in l:
            if '-' == item[0]:
                num = l.index(item)
                if num + 1 < len(l):
                    if '-' != l[num + 1][0]:
                        inputDic[item[1:]] = l[num + 1]
    outputDic = {}
    for key in inputDic:
        if key in keyReplace:
            outputDic[keyReplace[key]] = inputDic[key]
        if key in safeKeys:
            outputDic[key] = inputDic[key]
    return outputDic


class myThread(threading.Thread):
    def __init__(self, threadID, threadName, type='download', key=''):
        self.exitFlag = 0
        self.timeoutError = 0
        self.type = type
        self.download_path = './downloads/'
        threading.Thread.__init__(self)
        self.threadID = threadID
        self.threadName = threadName
        self.key = key
        self.missionList = []

    def download(self, threadName):
        while not self.exitFlag:
            queueLock.acquire()  # 保持线程同步
            if not workQueue.empty():
                m = workQueue.get()
                pdUrl = m[0]
                tsTime = m[1]
                missStr = m[2]
                queueLock.release()
                # 获取文件名，方便映射m3u8
                fuleName = pdUrl.rsplit("/", 1)[-1]
                fuleName = fuleName.rsplit("?", 1)[0]
                # print('♦♦♦♦♦♦♦♦♦♦♦♦♦♦♦♦♦♦♦♦')
                # print(pdUrl,fuleName)
                # time.sleep(3)
                if fuleName not in os.listdir(self.download_path):
                    resCount = 0
                    print('%s%s准备下载%s 保存为 %s' % (missStr, threadName, pdUrl, fuleName))
                    while resCount <= repeatMax:
                        try:
                            # 修改request模式为 url + params ,支持超长request
                            p = urllib.parse.urlparse(pdUrl)
                            u = p.scheme + '://' + p.netloc + p.path
                            pTemp = urllib.parse.parse_qs(p.query)
                            for i in pTemp:
                                pTemp[i] = pTemp[i][0]
                            res = requests.get(url=u, params=pTemp)
                            # res = requests.post(url=u, params=pTemp)
                            resC = res.content
                            break
                        except:
                            time.sleep(repeatWaitTime)
                            resCount += 1
                            resC = ''
                    if len(self.key):  # AES 解密
                        # print("================")
                        # print(self.key, AES.MODE_CBC, self.key)
                        cryptor = AES.new(self.key, AES.MODE_CBC, self.key)
                        # if '.' not in fuleName:
                            # fuleName += '.ts'
                        with open(os.path.join(self.download_path, fuleName), 'ab') as f:
                            f.write(cryptor.decrypt(resC))
                            result = (os.path.join(self.download_path, fuleName), tsTime)
                    else:
                        with open(os.path.join(self.download_path, fuleName), 'ab') as f:
                            f.write(resC)
                            f.flush()
                            result = (os.path.join(self.download_path, fuleName), tsTime)
                else:
                    result = (os.path.join(self.download_path, fuleName), tsTime)
            else:
                queueLock.release()
        return result

    def run(self):
        print("开启线程：" + self.threadName)
        if self.type == 'download':
            self.download(self.threadName)
        # 此处可以扩展根据业务不同执行不同的多线程，目前仅使用下载
        print("退出线程：" + self.threadName)


def doMission(missionList=[], threadNum=5, type='download', key=''):
    if missionList != []:
        threads = []
        threadID = 1
        # 创建新线程
        for i in range(threadNum):
            threadName = 'OPN48-Thread-' + str(i)
            thread = myThread(threadID, threadName, type, key)
            thread.start()
            threads.append(thread)
            threadID += 1

        queueLock.acquire()
        # 填充队列
        for m in missionList:
            workQueue.put(m)
        queueLock.release()
        # 等待队列清空
        while not workQueue.empty():
            # 这里可以放置监听程序，比如有鉴黄需求，监听数据库某影片已被百度鉴黄确定后，可以直接终止整个线程，即执行exitFlage=1
            pass

        # 等待所有线程完成
        for t in threads:
            # 通知线程是时候退出
            t.exitFlag = 1
            t.join(timeout=180)
        print("退出主线程")


class M3u8PParsing():
    def __init__(self):
        self.dlPath = './downloads/'
        self.tsDic = {}
        self.stepTsDic = {}
        self.key = ''
        self.info = ''
        self.downloadBaseNameList = []
        self.threadNum = 5

    def merge2Mp4(self, path, name='new'):
        if self.downloadBaseNameList:
            # 用于全流程直接使用
            filesList = self.downloadBaseNameList
        else:
            # 用于外部调用指定文件夹
            filesList = os.listdir(path)
            filesList.sort()
        os.chdir(path)
        files = ''
        # 部分下载文件可在此判断是会否成功，比如小于1KB忽略后合并

        for i in filesList:
            files += i + ' '
        cmd = 'cat ' + files + '> ' + name + '.tmp'
        os.system(cmd)
        os.system('rm -rf *.ts')
        os.system('rm -rf *.mp4')
        os.rename(name + ".tmp", name + ".mp4")

    def getTsdic(self, url):
        host = url[:url.find('/', url.find('//') + 2)]
        # all_content = requests.get(url).text  # 获取第一层M3U8文件内容
        all_content = requests.get(url).text  # 获取第一层M3U8文件内容
        if "#EXTM3U" not in all_content:
            raise BaseException("非M3U8的链接")
        if "EXT-X-STREAM-INF" in all_content:  # 第一层
            fileLine = all_content.split("\n")
            for line in fileLine:
                if 'EXT-X-STREAM-INF' in line:
                    self.info = re.findall(r'EXT-X-STREAM-INF:(.*?)$', line)[0]
                elif '.m3u8' in line:
                    if line[:1] == '/':
                        if line[:4] == 'http':
                            url = line
                        else:
                            url = host + line
                    else:
                        url = url.rsplit("/", 1)[0] + "/" + line  # 拼出第二层m3u8的URL
                    # print('EXT-X-STREAM-INF', url)
                    all_content = requests.post(url).text
                elif '#' not in line and line.strip():
                    url = host + line.strip()
                    # print('======else # not in line and line.strip()======', url)
                    all_content = requests.post(url).text
                    # print(all_content)
                else:
                    pass
        fileLine = all_content.split("\n")
        unknow = True
        t = 0.0
        # print(fileLine)
        for index, line in enumerate(fileLine):  # 第二层
            if "#EXT-X-KEY" in line:  # 找解密Key
                unknow = False
                methodPos = line.find("METHOD")
                commaPoS = line.find(",")
                method = line[methodPos:commaPoS].split('=')[1]
                print("Decode Method：", method)
                uri_pos = line.find("URI")
                quotation_mark_pos = line.rfind('"')
                key_path = line[uri_pos:quotation_mark_pos].split('"')[1]
                # print('======key_path=======',key_path)
                if key_path[:4] == 'http':
                    key_url = key_path
                elif key_path[:1] == '/':
                    key_url = host + key_path
                else:
                    key_url = url.rsplit("/", 1)[0] + "/" + key_path  # 拼出key解密密钥URL
                res = requests.get(key_url)
                if '404' in str(res.content):
                    print('======获取key失败======',key_url,res.content)
                self.key = res.content
                # print("======self.key======", self.key)
            if "EXTINF" in line:  # 找ts地址并下载
                unknow = False
                # ts时长
                length = float(re.findall(r'EXTINF:(.*?),', line.strip())[0])
                # 进度时长time
                beginTime = round(t, 3)
                t += length
                # url拼接模块
                if fileLine[index + 1][:1] == '/':
                    pdUrl = host + fileLine[index + 1]
                elif '://' in fileLine[index + 1]:
                    pdUrl = fileLine[index + 1]
                else:
                    pdUrl = url.rsplit("/", 1)[0] + "/" + fileLine[index + 1]  # 拼出ts片段的URL
                # 去除url回车
                pdUrl = pdUrl.strip()
                self.tsDic[beginTime] = {'url': pdUrl, 'length': length, 'file': fileLine[index + 1]}
        if unknow:
            raise BaseException("未找到对应的下载链接")
        else:
            print("下载字典输出完成")
        print("对字典进行排序生成downloadBaseNameList用于兼容不同系统文件排序")
        for beginTime in sorted(self.tsDic.keys()):
            self.downloadBaseNameList.append(os.path.basename(self.tsDic[beginTime]['file']))
        return self.tsDic

    def getStepDic(self, step):
        lasttime = 0.0
        if step == 0:
            self.stepTsDic = self.tsDic
        else:
            for i in self.tsDic:
                beginTime = i
                time = round(beginTime + self.tsDic[i]['length'])
                if (time - lasttime) >= step:
                    self.stepTsDic[i] = self.tsDic[i]
                    lasttime = time
        return self.stepTsDic

    def downloadM3u8(self, missionList):
        if missionList != []:
            if not os.path.exists(self.dlPath):
                os.makedirs(self.dlPath)
            finishTsList = os.listdir(self.dlPath)
            missionDownloadList = []
            for j in missionList:
                if j[0].rsplit("/", 1)[-1] not in finishTsList:
                    missionDownloadList.append(j)

            doMission(missionDownloadList, self.threadNum, type='download', key=self.key)
            tempList = []
            for i in missionList:
                path = self.dlPath + i[0].rsplit("/", 1)[-1]
                tempList.append((path, i[1]))
            return tempList
        else:
            return []

    def justDownload(self):
        missionDic = self.tsDic
        missionList = []
        missionNum = 1
        for i in missionDic:
            beginTime = i
            pdUrl = missionDic[i]['url']
            tsTime = str(beginTime)
            missStr = '(%s/%s)' % (missionNum, len(missionDic))
            missionList.append((pdUrl, tsTime, missStr))
            missionNum += 1
        self.downloadM3u8(missionList)


if __name__ == '__main__':
    modelDic = {
        # 参数名 :(命令缩写，msg功能描述，默认值)
        'url': ('u', 'm3u8下载链接', ''),
        'dlpath': ('d', '下载文件夹', 'downloads'),
        'type': ('t', '下载文件是否合并为mp4', 'mp4'),
        'name': ('n', '合并后mp4名称', 'new')
    }
    safeKeys = modelDic.keys()
    keyReplace = dict(zip([modelDic[i][0] for i in modelDic], list(safeKeys)))
    helpStr = '用法: python3 pyM3u8Download.py [-options] [args...](执行m3u8下载)\n'
    inputDic = getSysArgv(keyReplace, safeKeys)
    if not inputDic:
        # 输出帮助
        print(helpStr)
    else:
        # 指令合并
        mDic = {}
        for i in modelDic:
            mDic[i] = modelDic[i][2]
        for i in inputDic:
            mDic[i] = inputDic[i]
        print(mDic)
        url = mDic['url']
        x = M3u8PParsing()
        x.dlPath = mDic['dlpath']
        x.getTsdic(url)
        x.justDownload()
        if mDic['type'] == 'mp4':
            x.merge2Mp4(x.dlPath, name=mDic['name'])
