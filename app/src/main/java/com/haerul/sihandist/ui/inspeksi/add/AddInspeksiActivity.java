package com.haerul.sihandist.ui.inspeksi.add;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.snackbar.Snackbar;
import com.haerul.sihandist.R;
import com.haerul.sihandist.base.BaseActivity;
import com.haerul.sihandist.data.api.ConnectionServer;
import com.haerul.sihandist.data.db.repository.MasterRepository;
import com.haerul.sihandist.data.entity.Base64Data;
import com.haerul.sihandist.data.entity.Inspeksi;
import com.haerul.sihandist.databinding.ActivityInspeksiAddBinding;
import com.haerul.sihandist.utils.MapActivity;
import com.haerul.sihandist.utils.CameraXActivity;
import com.haerul.sihandist.utils.Constants;
import com.haerul.sihandist.utils.Util;

import org.json.JSONObject;

import java.io.File;

import javax.inject.Inject;

public class AddInspeksiActivity extends BaseActivity<ActivityInspeksiAddBinding, AddInspeksiViewModel>
        implements AddInspeksiViewModel.Navigator {

    private static final int REQUEST_CODE_LOC = 0x291;
    private static final int REQUEST_CAMERA = 0x218;
    @Inject
    ConnectionServer connectionServer;
    @Inject
    MasterRepository repository;

    private ActivityInspeksiAddBinding binding;
    private AddInspeksiViewModel viewModel;
    private String fileUri = null;
    private String imageBase64 = null;
    private Inspeksi inspeksi = null;

    @Override
    public int getBindingVariable() {
        return com.haerul.sihandist.BR._all;
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_inspeksi_add;
    }

    @Override
    public AddInspeksiViewModel getViewModel() {
        return viewModel;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = getViewDataBinding();
        viewModel = ViewModelProviders.of(this, new AddInspeksiViewModel.ModelFactory(this, connectionServer, repository)).get(AddInspeksiViewModel.class);
        viewModel.setNavigator(this);

        if (getIntent().getSerializableExtra(Constants.EXTRA_DATA) != null) {
            inspeksi = (Inspeksi) getIntent().getSerializableExtra(Constants.EXTRA_DATA);
            assert inspeksi != null;
            binding.toolbar.setTitle("" + inspeksi.inspeksi_uid);

            binding.setItem(inspeksi);
            binding.setViewModel(viewModel);
            binding.rayon.setSpinnerValue(repository, Constants.ULP, inspeksi.rayon_sid);
            binding.rayon.setEnabled(false);
            binding.jenisTemuan.setSpinnerValue(repository, Constants.JENIS_TEMUAN, inspeksi.jenis_temuan_sid);
            binding.jenisTemuan.setEnabled(false);
            binding.tingkatEmergency.setRadioGroupValue(repository, Constants.KONDISI_TINGKAT_EMERGENCY, inspeksi.tingkat_emergency_sid, false);
            binding.pemadaman.setRadioGroupValue(repository, Constants.PEMADAMAN, inspeksi.pemadaman_sid, false);
            binding.penyulang.setSpinnerValue(repository, Constants.PENYULANG, inspeksi.penyulang_sid);
            binding.penyulang.setEnabled(false);

            //map snapshot
            if (inspeksi.lokasi_inspeksi_y != null) {
                Util.loadMapView(binding.mapView, inspeksi.lokasi_inspeksi_y, inspeksi.lokasi_inspeksi_x);
                binding.mapPreviewLayout.setVisibility(View.VISIBLE);
            }

            if (repository.getBase64Data(inspeksi.foto_inspeksi) != null) {
                Base64Data base64data = repository.getBase64Data(inspeksi.foto_inspeksi);
                Util.setImageBase64Preview(base64data, binding.photoPreview);
                binding.photoText.setText(base64data.data_path.replaceAll(Constants.PATH_IMG, ""));
            }
            
            binding.cancel.setVisibility(View.GONE);
            binding.save.setVisibility(View.GONE);
        } 
        else {
            
            binding.toolbar.setTitle("Add New Record");
            binding.noInspeksi.setText("INS-000000-00000");
            binding.noInspeksi.setEnabled(false);
            binding.tanggalInspeksi.setText(Util.dateFormatter(Util.getTimestampNow(), Constants.DATE_ONLY_FORMAT));
            binding.jamInspeksi.setText(Util.dateFormatter(Util.getTimestampNow(), Constants.TIME_ONLY_FORMAT));
            binding.tingkatEmergency.setupRadioGroup(repository, Constants.KONDISI_TINGKAT_EMERGENCY);
            binding.pemadaman.setupRadioGroup(repository, Constants.PEMADAMAN);
            binding.lokasiInspeksi.setOnClickListener(view1 -> pickLocation());
            binding.setLokasi.setOnClickListener(view1 -> pickLocation());
            binding.photoText.setOnClickListener(view12 -> takePhoto());
            binding.photo.setOnClickListener(view12 -> takePhoto());
            binding.photoPreview.setOnClickListener(view12 -> takePhoto());
            binding.rayon.setupSpinnerGeneric(repository, Constants.ULP);
            binding.rayon.setSpinnerValue(repository, Constants.ULP, Util.getStringPreference(this, Constants.USER_UNIT));
            binding.rayon.setEnabled(false);
            binding.penyulang.setupSpinnerGeneric(repository, Constants.PENYULANG, Util.getStringPreference(this, Constants.USER_UNIT));
            binding.jenisTemuan.setupSpinnerGeneric(repository, Constants.JENIS_TEMUAN);

            binding.cancel.setOnClickListener(v -> {
                finish();
            });

            binding.save.setOnClickListener(v -> {
                hideKeyboard();
                saveInspekse();
            });
        }

        setupToolbar();
    }

    private void saveInspekse() {
        if (Util.isNullOrEmpty(binding.jamInspeksi.getText().toString())) {
            binding.jamInspeksi.setError("This field can't be empty!");
            binding.jamInspeksi.requestFocus();
            Util.scrollToView(binding.jamInspeksi, binding.scrollView);
        } else if (Util.isNullOrEmpty(binding.tanggalInspeksi.getText().toString())) {
            binding.tanggalInspeksi.setError("This field can't be empty!");
            binding.tanggalInspeksi.requestFocus();
            Util.scrollToView(binding.tanggalInspeksi, binding.scrollView);
        } else if (Util.isNullOrEmpty(binding.rayon.getSpinnerValueSID(repository, Constants.ULP))) {
            Util.setErrorSipnner(binding.rayon, "This field can't be empty!");
            Util.scrollToView(binding.rayon, binding.scrollView);
        } else if (Util.isNullOrEmpty(binding.penyulang.getSpinnerValueSID(repository, Constants.PENYULANG))) {
            Util.setErrorSipnner(binding.penyulang, "This field can't be empty!");
            Util.scrollToView(binding.penyulang, binding.scrollView);
        } else if (Util.isNullOrEmpty(binding.jenisTemuan.getSpinnerValueSID(repository, Constants.JENIS_TEMUAN))) {
            Util.setErrorSipnner(binding.jenisTemuan, "This field can't be empty!");
            Util.scrollToView(binding.jenisTemuan, binding.scrollView);
        } else if (binding.tingkatEmergency.getCheckedRadioButtonId() == -1) {
            Snackbar.make(binding.getRoot(), "Please select a Tingkat Emergency", Snackbar.LENGTH_LONG).show();
            Util.scrollToView(binding.tingkatEmergency, binding.scrollView);
            Util.setFadeBackgroundError(binding.tingkatEmergency);
        } else if (binding.pemadaman.getCheckedRadioButtonId() == -1) {
            Snackbar.make(binding.getRoot(), "Please select a Pemadaman", Snackbar.LENGTH_LONG).show();
            Util.scrollToView(binding.pemadaman, binding.scrollView);
            Util.setFadeBackgroundError(binding.pemadaman);
        } else if (Util.isNullOrEmpty(binding.lokasiInspeksi.getText().toString())) {
            Snackbar.make(binding.getRoot(), "Please pick a Lokasi Inspeksi", Snackbar.LENGTH_LONG).show();
            Util.scrollToView(binding.lokasiInspeksi, binding.scrollView);
            Util.setFadeBackgroundError(binding.layoutLokasi);
        } else if (Util.isNullOrEmpty(binding.photoText.getText().toString())) {
            Snackbar.make(binding.getRoot(), "Please take a Photo Inspeksi", Snackbar.LENGTH_LONG).show();
            Util.scrollToView(binding.photoText, binding.scrollView);
            Util.setFadeBackgroundError(binding.layoutFoto);
        } else {
            Inspeksi inspeksi = new Inspeksi();
            inspeksi.inspeksi_sid = Util.createSID();
            inspeksi.rayon_sid = binding.rayon.getSpinnerValueSID(repository, Constants.ULP);
            inspeksi.penyulang_sid = binding.penyulang.getSpinnerValueSID(repository, Constants.PENYULANG, Util.getStringPreference(this, Constants.USER_UNIT));
            inspeksi.jenis_temuan_sid = binding.jenisTemuan.getSpinnerValueSID(repository, Constants.JENIS_TEMUAN);
            inspeksi.jenis_wo_sid = binding.jenisTemuan.getSpinnerValue(repository, Constants.JENIS_TEMUAN).ref_description;
            inspeksi.tingkat_emergency_sid = binding.tingkatEmergency.getRadioGroupValueSID(repository, Constants.KONDISI_TINGKAT_EMERGENCY);
            inspeksi.pemadaman_sid = binding.pemadaman.getRadioGroupValueSID(repository, Constants.PEMADAMAN);
            inspeksi.lokasi_inspeksi_x = viewModel.lon;
            inspeksi.lokasi_inspeksi_y = viewModel.lat;
            inspeksi.keterangan = binding.keterangan.getText().toString();
            inspeksi.tanggal_inspeksi = binding.tanggalInspeksi.getText().toString() + " " + binding.jamInspeksi.getText().toString() + ":00";
            Base64Data dataBase64 = Util.insertBase64(repository, imageBase64, Constants.PATH_IMG + binding.photoText.getText().toString(), Util.getUserSID(this));
            inspeksi.foto_inspeksi = dataBase64.data_sid;
            inspeksi.post_by = Util.getUserSID(this);
            viewModel.postInspeksi(inspeksi);
            viewModel.postBase64Data(dataBase64);
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void pickLocation() {
        Log.d("TAG", "addLocation");
        try {
            JSONObject LonLat = new JSONObject();

            LonLat.put("LONGITUDE", Double.parseDouble(viewModel.lon.equals("") ? "0" : viewModel.lon));
            LonLat.put("LATITUDE", Double.parseDouble(viewModel.lat.equals("") ? "0" : viewModel.lat));

            Log.w("TAG", LonLat.toString());

            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra("JSON", String.valueOf(LonLat));
            Log.w("TAG pick", String.valueOf(LonLat));
            startActivityForResult(intent, REQUEST_CODE_LOC);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_LOC) {
            if (resultCode == RESULT_OK) {
                try {
                    Log.wtf("LOCATION", "EDIT");
                    JSONObject jsons = new JSONObject(data.getStringExtra("JSON"));
                    viewModel.lon = String.valueOf(jsons.getDouble("LONGITUDE"));
                    viewModel.lat = String.valueOf(jsons.getDouble("LATITUDE"));
                    binding.lokasiInspeksi.setText(String.format("%s, %s", viewModel.lon, viewModel.lat));
                    Util.loadMapView(binding.mapView, viewModel.lat, viewModel.lon);
                    binding.mapPreviewLayout.setVisibility(View.VISIBLE);
                    Log.w("TAG 2", String.valueOf(jsons));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (requestCode == REQUEST_CAMERA) {
            if (resultCode == RESULT_OK) {
                try {
                    Log.w("Test", data.toString());
                    File file = new File(data.getStringExtra("data"));
                    fileUri = Uri.fromFile(file).toString();
                    imageBase64 = viewModel.previewCapturedImage(binding.photoPreview, fileUri);
                    binding.photoText.setText("" + file.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void takePhoto() {
        Intent intent = new Intent(this, CameraXActivity.class);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @Override
    public void result(boolean status, String message) {
        hideKeyboard();
        if (status) {
            finish();
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
        } else {
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }
}
