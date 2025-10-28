package net.ib.mn.emoticon

import android.content.Context
import net.htmlparser.jericho.HTMLElementName.DIR
import net.ib.mn.addon.IdolGson
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.EmoticonDetailModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import org.json.JSONObject
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ZipManager private constructor(){

    //모든 이모티콘 파일 정보.
    private var emoticonAllInfoList:ArrayList<EmoticonDetailModel> = ArrayList()

    //zip파일 풀기.
    fun unzip(context: Context, zipFileName:String, targetLocation: String, id:Int){
        val zipFile = File(zipFileName)
        var fis:FileInputStream? = null
        var zis:ZipInputStream? = null
        var zipentry:ZipEntry
        val gson = IdolGson.getInstance()

        try{
            //내부저장소에서 zip파일 읽어옴.
            fis = FileInputStream(zipFile)
            zis = ZipInputStream(fis)

            //zip파일 내부에있는 파일 하나하나 전부 확인(다음 파일이 있을때까지 계속 실행해준다).
            while (zis.nextEntry.also { zipentry = it } != null) {
                //해당 파일이름가져온다음에 원하는 위치에 파일 경로를 만들음.
                val filename = zipentry.name
                val file = File(targetLocation, filename)
                val canonicalPath = file.canonicalPath
                if(!canonicalPath.startsWith(file.path)) {
                    ensureZipPathSafety(file, targetLocation)
                }else{
                    if (zipentry.isDirectory) {
                        file.mkdirs()
                    } else {
                        createFile(context, file, zis, zipentry, id, targetLocation)
                    }
                }

            }
        } catch (e:Exception){
            e.printStackTrace()
        } finally {
            //createFile에서 해당 파일을 만들어준다음에 마지막으로 이모티콘 모든정보를 로컬캐싱.
            Util.setPreference(context, Const.EMOTICON_ALL_INFO, gson.toJson(emoticonAllInfoList))
            zis?.close()
            fis?.close()
        }
    }

    private fun ensureZipPathSafety(file : File, desDirectory : String){
        val destCanonicalPath = File(desDirectory).canonicalPath
        val outputCanonicalPath = file.canonicalPath

        if(!outputCanonicalPath.startsWith(destCanonicalPath)){
            throw Exception(String.format("Found Zip Path Traversal Vulnerability"))
        }

    }

    //zip 안에있는 파일 만들어줌.
    private fun createFile(context: Context, file: File, zis: ZipInputStream, zipentry: ZipEntry, id: Int, targetLocation: String) {
        val gson = IdolGson.getInstance()

        val parentDir = File(file.parent)
        //디렌토리가 없으면 생성.
        if(!parentDir.exists()){
            parentDir.mkdirs()
        }

        try{
           val fos = FileOutputStream(file)

            val buffer = ByteArray(1024)
            var size: Int

            //파일 끝까지 써줌.
            while (zis.read(buffer).also { size = it } > 0) {
                fos.write(buffer, 0, size)
            }

            fos.close()

            //json파일 찾아줘서 리스트에 넣어줍니다.
            val jsonFile = File(file.absolutePath)
            val jsonName = jsonFile.name
            val ext = jsonName.substring(jsonName.lastIndexOf(".") + 1)
            if (ext == "json" && !jsonFile.name.startsWith(".")) { //json일떄.
                val model = JSONObject(getStringFromFile(jsonFile))

                var list = model.getJSONArray("emoticons")
                for (i in 0 until list.length()) {
                    val model = gson.fromJson(list[i].toString(), EmoticonDetailModel::class.java)
                    model.emoticonSetId = id
                    model.isSetCategoryImg = false//카테고리용 값인지 체크한다.

                    //Config의 emoticonUrl가져옴.
                    val imageStr = UtilK.fileImageUrl(context, model.id)
                    val thumbStr = UtilK.fileImageUrl(context, model.id)

                    //마지막으로 수정한 imageUrl,thumbnail을 다시한번 초기화 시켜줌.
                    model.imageUrl = imageStr
                    model.thumbnail = thumbStr

                    model.filePath = targetLocation  + File.separator + id + File.separator + "${model.id}" //이모티콘 filePath저장.
                    emoticonAllInfoList.add(model)
                }

                //이모티콘 카테고리별 set 이미지 적용을 위해 list에  카테고리값용 모델을 추가해줌.
                emoticonAllInfoList.add(EmoticonDetailModel("", -1, "", "", "on", -1, id, targetLocation + File.separator + id + File.separator + "${id}_on", true))
                emoticonAllInfoList.add(EmoticonDetailModel("", -1, "", "", "off", -1, id, targetLocation + File.separator + id + File.separator + "${id}_off", true))
            }
        } catch (e:Exception){
            e.printStackTrace()
        }
    }

    //JSON 파일에있는 정보들을 String으로 받아옴.
    @Throws(Exception::class)
    fun getStringFromFile(file: File?): String? {
        val fin = FileInputStream(file)
        val ret: String = convertStreamToString(fin)!!
        //Make sure you close all streams.
        fin.close()
        return ret
    }

    @Throws(java.lang.Exception::class)
    fun convertStreamToString(`is`: InputStream?): String? {
        val reader = BufferedReader(InputStreamReader(`is`))
        val sb = StringBuilder()
        var line: String? = null
        while (reader.readLine().also { line = it } != null) {
            sb.append(line).append("\n")
        }
        reader.close()
        return sb.toString()
    }

    companion object{

        @Volatile
        private var instance: ZipManager? = null

        //이모티콘 전체정보가 매번달라지기때문에 싱글턴으로 만들어 인스턴스 여러개 만드는거 방지.
        @JvmStatic
        @Synchronized
        fun getInstance(emoticonAllInfoList: ArrayList<EmoticonDetailModel>?): ZipManager? {
            if(instance == null){
                synchronized(ZipManager::class.java){
                    instance = ZipManager()
                }
            }

            if (emoticonAllInfoList != null) {
                instance!!.emoticonAllInfoList = emoticonAllInfoList
            }

            return instance
        }

    }
}
