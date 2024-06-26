package alvarenga.brandao.galeria;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import alvarenga.brandao.galeria.R;

public class MainActivity extends AppCompatActivity {
    List<String> photos = new ArrayList<>();
    MainAdapter mainAdapter;
    static int RESULT_TAKE_PICTURE = 1;
    static int RESULT_REQUEST_PERMISSION = 2;
    String currentPhotoPath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // lista de permissoes do usuario
        List<String> permissions = new ArrayList<>();
        // adiciona permissao para a camera
        permissions.add(Manifest.permission.CAMERA);
        // verifica as permissoes
        checkForPermissions(permissions);

        Toolbar toolbar = findViewById(R.id.tbMain);
        // adiciona a toolbar
        setSupportActionBar(toolbar);

        // acessa a pasta do aplicativo
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        // lista com as fotos que estao no diretorio
        File[] files = dir.listFiles();

        // loop para adicionar o caminho das fotos na lista de paths
        for(int i = 0; i < files.length; i++) {
            photos.add(files[i].getAbsolutePath());
        }
        mainAdapter = new MainAdapter(MainActivity.this, photos);
        RecyclerView rvGallery = findViewById(R.id.rvGallery);
        rvGallery.setAdapter(mainAdapter);
        float w = getResources().getDimension(R.dimen.itemWidth);
        int numberOfColumns = Util.calculateNoOfColumns(MainActivity.this, w);

        // grid em colunas
        GridLayoutManager gridLayoutManager = new GridLayoutManager(MainActivity.this, numberOfColumns);
        rvGallery.setLayoutManager(gridLayoutManager);
    }
    public void dispatchTakePictureIntent() {
        File f = null;
        // tenta criar um arquivo de imagem vazio
        try {
            f = createImageFile();
        } catch (IOException e) {
            // erro ao criar o arquivo
            Toast.makeText(MainActivity.this, "Não foi possível criar o arquivo", Toast.LENGTH_LONG).show();
            return;
        }

        // pega o caminho da foto
        currentPhotoPath = f.getAbsolutePath();

        if(f == null) return;
        Uri fUri = FileProvider.getUriForFile(MainActivity.this, "alvarenga.brandao.galeria.fileprovider", f);

        // Intent para capturar a foto
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        i.putExtra(MediaStore.EXTRA_OUTPUT, fUri);
        startActivityForResult(i, RESULT_TAKE_PICTURE);
    }

    private File createImageFile() throws IOException{
        // utiliza a data para nomear o arquivo
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp;

        // acessa o diretorio de imagens
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File f = File.createTempFile(imageFileName, ".jpg", storageDir);
        return f;
    }

    private void checkForPermissions(List<String> permissions) {
        List<String> permissionsNotGranted = new ArrayList<>();
        // verifica se as permissoes nao foram dadas
        for(String permission : permissions) {
            if (!hasPermission(permission)) {
                permissionsNotGranted.add(permission);
            }
        }

        // pede a permissao que ainda nao foram concedidas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsNotGranted.size() > 0) {
                requestPermissions(permissionsNotGranted.toArray(new String[permissionsNotGranted.size()]), RESULT_REQUEST_PERMISSION);
            }
        }
    }

    private boolean hasPermission(String permission) {
        // testa se a permissao foi concedida
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ActivityCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_GRANTED;
            }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        final List<String> permissionsRejected = new ArrayList<>();

        if (requestCode == RESULT_REQUEST_PERMISSION) {

            // permissoes negadas
            for(String permission : permissions) {
                if (!hasPermission(permission)) {
                    permissionsRejected.add(permission);
                }
            }
        }

        if (permissionsRejected.size() > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                // Checa se podemos pedir permissoes ao usuario
                if(shouldShowRequestPermissionRationale(permissionsRejected.get(0)))
                {
                    new AlertDialog.Builder(MainActivity.this).setMessage("Para usar essa epp é preciso conceder essas permissões").setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(permissionsRejected.toArray(new String[permissionsRejected.size()]), RESULT_REQUEST_PERMISSION);
                        }
                    }).create().show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != RESULT_TAKE_PICTURE) return;
        if(resultCode == Activity.RESULT_OK) {
            photos.add(currentPhotoPath);
            mainAdapter.notifyItemInserted(photos.size()-1);
        } else {
            // apaga o arquivo caso a tela nao abra corretamente
            File f = new File(currentPhotoPath);
            f.delete();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        // infla a view do menu
        inflater.inflate(R.menu.main_activity_tb, menu);
        return true;
    }

    @Override // chamada quando o item da toolbar é selecionado
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.opCamera) {
            // tira a foto
            dispatchTakePictureIntent();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void startPhotoActivity(String photoPath) {
        Intent i = new Intent(MainActivity.this, PhotoActivity.class);
        // passa o caminho da foto
        i.putExtra("photo_path", photoPath);
        startActivity(i);
    }
}