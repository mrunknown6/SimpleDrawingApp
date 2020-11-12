package com.example.simpledrawingapp

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log.d
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_brush_size.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    var ibCurrentPaint: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dv.setSizeForBrush(20f)

        ibCurrentPaint = llPaintColors[1] as ImageButton
        ibCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_selected)
        )

        ibBrush.setOnClickListener {
            showBrushSizeDialog()
        }

        ibUndo.setOnClickListener {
            dv.onClickUndo()
        }

        ibGallery.setOnClickListener {
            if (isReadStorageAllowed()) {
                val pickPhotoIntent = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )
                startActivityForResult(pickPhotoIntent, GALLERY)
            } else {
                requestStoragePermission()
            }
        }

        ibSave.setOnClickListener {
            if (isReadStorageAllowed()) {
                BitmapAsyncTask(getBitmapFromView(flContainer)).execute()
            } else {
                requestStoragePermission()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == GALLERY) {
                try {
                    if (data!!.data != null) {
                        ivBackground.apply {
                            visibility = View.VISIBLE
                            setImageURI(data.data)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showBrushSizeDialog() {
        val brushDialog = Dialog(this)
        brushDialog.apply {
            setContentView(R.layout.dialog_brush_size)
            setTitle("Brush size: ")
            ibSmallBrush.setOnClickListener {
                this@MainActivity.findViewById<DrawingView>(R.id.dv).setSizeForBrush(10f)
                dismiss()
            }
            ibMediumBrush.setOnClickListener {
                this@MainActivity.findViewById<DrawingView>(R.id.dv).setSizeForBrush(20f)
                dismiss()
            }
            ibLargeBrush.setOnClickListener {
                this@MainActivity.findViewById<DrawingView>(R.id.dv).setSizeForBrush(30f)
                dismiss()
            }
            show()
        }
    }

    fun paintClicked(v: View) {
        if (v !== ibCurrentPaint) {
            val ib = v as ImageButton
            val tag = ib.tag.toString()
            dv.setColor(tag)
            ib.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_selected))
            ibCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.pallet_normal
                )
            )
            ibCurrentPaint = ib
        }
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).toString()
            )
        ) {
            Toast.makeText(this, "Need permission to add a background", Toast.LENGTH_SHORT).show()
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            STORAGE_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    "Permission granted",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "You denied the permission",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun isReadStorageAllowed(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun getBitmapFromView(v: View): Bitmap {
        val returnBitmap = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnBitmap)
        val bgDrawable = v.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        v.draw(canvas)
        return returnBitmap
    }

    private inner class BitmapAsyncTask(val bitmap: Bitmap) : AsyncTask<Any, Void, String>() {

        private lateinit var progressDialog: Dialog

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }

        override fun doInBackground(vararg params: Any?): String {
            var result = ""

            try {
                val bytes = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
                val file =
                    File(
                        externalCacheDir!!.absoluteFile.toString() +
                                File.separator + "SimpleDrawingApp_" +
                                System.currentTimeMillis() / 1000 + ".png"
                    )
                val fos = FileOutputStream(file)
                fos.write(bytes.toByteArray())
                fos.close()
                result = file.absolutePath
            } catch (e: Exception) {
                result = ""
                e.printStackTrace()
            }
            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            cancelProgressDialog()
            if (result!!.isNotEmpty()!!) {
                Toast.makeText(
                    this@MainActivity,
                    "File saved successfully: $result",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Something went wrong.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            MediaScannerConnection.scanFile(
                this@MainActivity,
                arrayOf(result),
                null
            ) { path, uri ->
                val shareIntent = Intent()
                shareIntent.apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = "image/png"
                }
                startActivity(Intent.createChooser(shareIntent, "Share"))
            }
        }

        private fun showProgressDialog() {
            progressDialog = Dialog(this@MainActivity)
            progressDialog.setContentView(R.layout.dialog_custom_progress)

            progressDialog.show()
        }

        private fun cancelProgressDialog() {
            progressDialog.dismiss()
        }
    }

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }
}