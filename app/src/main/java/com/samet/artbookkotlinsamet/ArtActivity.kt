package com.samet.artbookkotlinsamet

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.samet.artbookkotlinsamet.databinding.ActivityArtBinding
import java.io.ByteArrayOutputStream

class ArtActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArtBinding
    private lateinit var activityResultLauncher:ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher:ActivityResultLauncher<String>
    var selectedBitmap:Bitmap?=null
    private lateinit var database:SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        database=this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)
        registerLauncher()

        val intent=intent
        val info=intent.getStringExtra("info")
        if (info.equals("new")){
            binding.artNameText.setText("")
            binding.artistNameText.setText("")
            binding.yearText.setText("")
            binding.button.visibility=View.VISIBLE
            binding.imageView.setImageResource(R.drawable.selecetp)
        }else{
            binding.button.visibility=View.INVISIBLE
            val selectedId=intent.getIntExtra("id",1)

            val cursor=database.rawQuery("SELECT*FROM arts WHERE id=?", arrayOf(selectedId.toString()))

            val artNameIx=cursor.getColumnIndex("artname")
            val artistNameIx=cursor.getColumnIndex("artistname")
            val yearIx=cursor.getColumnIndex("year")
            val imageIx=cursor.getColumnIndex("image")

            while (cursor.moveToNext()){
                binding.artNameText.setText(cursor.getString(artNameIx))
                binding.artistNameText.setText(cursor.getString(artistNameIx))
                binding.yearText.setText(cursor.getString(yearIx))

                val byteArray=cursor.getBlob(imageIx)
                val bitmap=BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                binding.imageView.setImageBitmap(bitmap)
            }

            cursor.close()



        }




    }
    fun saveButtonClicked(view: View){

        val artName=binding.artNameText.text.toString()
        val artistName=binding.artistNameText.text.toString()
        val year=binding.yearText.text.toString()

        if (selectedBitmap != null){
            val smallBitmap=makeSmallerBitmap(selectedBitmap!!, maximumSize = 300)
            //gorseli veriye cevirmek
            val outputStream=ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteArray=outputStream.toByteArray()


            try {
                val database=this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)
                database.execSQL("CREATE TABLE IF NOT EXISTS arts(id INTEGER PRIMARY KEY,artname VARCHAR,artistname VARCHAR,year VARCHAR,image BLOB)")

                val sqlString="INSERT INTO arts(artname,artistname,year,image)VALUES(?,?,?,?)"
                val statement=database.compileStatement(sqlString)
                statement.bindString(1,artName)
                statement.bindString(2,artistName)
                statement.bindString(3,year)
                statement.bindBlob(4,byteArray)
                statement.execute()


            }catch (e:Exception){
                e.printStackTrace()
            }

            val intent=Intent(this@ArtActivity,MainActivity::class.java)
            //Bundan once ne kadar acik aktivite varsa hepsini kapat demek
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)

        }

    }
    //Bitmap Smaller
    private fun makeSmallerBitmap(image:Bitmap,maximumSize:Int):Bitmap{

        var width=image.width
        var height=image.height

        val bitmapRatio:Double=width.toDouble()/height.toDouble()
        if (bitmapRatio>1){
            //Landscape yatay
            width=maximumSize
            val scaleHeight=width/bitmapRatio
            height=scaleHeight.toInt()
        }else{
            //portrait dikey
            height=maximumSize
            val scaleWidth=height*bitmapRatio
            width=scaleWidth.toInt()


        }

        return Bitmap.createScaledBitmap(image,100,100,true)
    }


    fun selectImage(view:View){

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                Snackbar.make(view,"Permission Needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission",View.OnClickListener {
                    //request permission
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)

                }).show()
            }else{
                //request permission
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }


        }else{
            val intentToGallery=Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGallery)


        }

    }
    //Galeriye gitmek ve galeriden veri cekmek
    private fun registerLauncher(){
        activityResultLauncher=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->

            if (result.resultCode== RESULT_OK){
                val intentFromResult = result.data
                if (intentFromResult!=null){
                    val imageData=intentFromResult.data
                    //binding.imageView.setImageURI(imageData)
                    if (imageData!=null) {
                        try {
                            if (Build.VERSION.SDK_INT>=28){
                                val source = ImageDecoder.createSource(contentResolver, imageData)
                                selectedBitmap=ImageDecoder.decodeBitmap(source)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }else{
                                selectedBitmap=MediaStore.Images.Media.getBitmap(contentResolver,imageData)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }



                }
            }

        }
        permissionLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission(),){result->
            if (result){
                //permission granted
                val intentToGallery=Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)

            }else{
                //permission denied
                Toast.makeText(this@ArtActivity,"Permission needed!",Toast.LENGTH_LONG).show()
            }
        }
    }
}