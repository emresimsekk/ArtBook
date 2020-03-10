package emresimsek.com;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AddArt extends AppCompatActivity {
    Bitmap selectedImage;
    ImageView imageView;
    EditText txtArtName,txtPainterName,txtYear;
    Button btnSave;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_art);

        imageView=findViewById(R.id.imageView);
        txtArtName=findViewById(R.id.txtArtName);
        txtPainterName=findViewById(R.id.txtPainterName);
        txtYear=findViewById(R.id.txtYearName);
        btnSave=findViewById(R.id.btnSave);
        database=this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);

        Intent intent=getIntent();
        String info=intent.getStringExtra("info");

        if (info.matches("new")){
            txtArtName.setText("");
            txtPainterName.setText("");
            txtYear.setText("");
            btnSave.setVisibility(View.VISIBLE);
            Bitmap selectImage= BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.select);
            imageView.setImageBitmap(selectImage);
        }
        else
        {
            int artId=intent.getIntExtra("artId",1);
            btnSave.setVisibility(View.INVISIBLE);
            try {
                Cursor cursor=database.rawQuery("SELECT * FROM arts WHERE id=?",new String[]{String.valueOf(artId)});
                int artNameIx=cursor.getColumnIndex("artname");
                int painterNameIx=cursor.getColumnIndex("paintername");
                int yearIx=cursor.getColumnIndex("year");
                int imageIx=cursor.getColumnIndex("image");

                while (cursor.moveToNext())
                {
                    txtArtName.setText(cursor.getString(artNameIx));
                    txtPainterName.setText(cursor.getString(painterNameIx));
                    txtYear.setText(cursor.getString(yearIx));

                    byte [] bytes = cursor.getBlob(imageIx);
                    Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    imageView.setImageBitmap(bitmap);
                }
            }
            catch (Exception e){

            }
        }

    }
    public void selectImage(View view){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)        //kullanıcı izni varmı sorgulama
        {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1); //kullanıcıdan izin isteme 1 ile istedik
        }
        else{
            Intent intentToGallery=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);//beni al galeriye götür
            startActivityForResult(intentToGallery,2); //sonuç için activity başlatıyoruz
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode==1)
        {

            if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) // içeriinde eleman varsa  eğer izin verildi isi
            {
                Intent intentToGallery=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);//beni al galeriye götür
                startActivityForResult(intentToGallery,2);  //sonuç için activity başlatıyoruz
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode==2 && resultCode==RESULT_OK&& data!=null)
        {
           Uri imageData=data.getData();
            try {
                if (Build.VERSION.SDK_INT>=28){
                    ImageDecoder.Source source= ImageDecoder.createSource(this.getContentResolver(),imageData);
                    selectedImage=ImageDecoder.decodeBitmap(source);
                    imageView.setImageBitmap(selectedImage);

                }
                else{
                    selectedImage=MediaStore.Images.Media.getBitmap(this.getContentResolver(),imageData);
                    imageView.setImageBitmap(selectedImage);

                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public  void save(View view){
        String artname=txtArtName.getText().toString();
        String painterName=txtPainterName.getText().toString();
        String year=txtYear.getText().toString();

            Bitmap smallImage=makeSmallerImage(selectedImage,300);

        // resim boyutları küçültüldü
        ByteArrayOutputStream outputStream =new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray=outputStream.toByteArray();

        try {
            database=this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);
            database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY,artname VARCHAR,paintername VARCHAR,year VARCHAR,image BLOB)");

            String sqlString="INSERT INTO arts (artname,paintername,year,image) VALUES (?,?,?,?)";
            SQLiteStatement sqLiteStatement=database.compileStatement(sqlString);
            sqLiteStatement.bindString(1,artname);
            sqLiteStatement.bindString(2,painterName);
            sqLiteStatement.bindString(3,year);
            sqLiteStatement.bindBlob(4,byteArray);
            sqLiteStatement.execute();


        }
        catch (Exception e)
        {

        }
        Intent intent=new Intent(AddArt.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        //finish();
    }
    //Görsel boyuta göre ayarlama küçültülmüş veriyor
    public Bitmap makeSmallerImage(Bitmap image,int maximumSize)
    {
        int width=image.getWidth();
        int height=image.getHeight();

        float bitmapRatio=(float) width/ (float) height ;
        if (bitmapRatio>1)
        {
            width=maximumSize;
            height=(int) (width/bitmapRatio);
        }else
        {
            height=maximumSize;
            width= (int)(height*bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image,width,height,true);
    }
}
