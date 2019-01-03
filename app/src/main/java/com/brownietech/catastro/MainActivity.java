package com.brownietech.catastro;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.glob3.mobile.generated.AltitudeMode;
import org.glob3.mobile.generated.Angle;
import org.glob3.mobile.generated.G3MContext;
import org.glob3.mobile.generated.G3MEventContext;
import org.glob3.mobile.generated.GInitializationTask;
import org.glob3.mobile.generated.GTask;
import org.glob3.mobile.generated.Geodetic3D;
import org.glob3.mobile.generated.IThreadUtils;
import org.glob3.mobile.generated.LayerSet;
import org.glob3.mobile.generated.LayerTouchEvent;
import org.glob3.mobile.generated.LayerTouchEventListener;
import org.glob3.mobile.generated.LevelTileCondition;
import org.glob3.mobile.generated.Mark;
import org.glob3.mobile.generated.MarksRenderer;
import org.glob3.mobile.generated.MercatorUtils;
import org.glob3.mobile.generated.Sector;
import org.glob3.mobile.generated.TimeInterval;
import org.glob3.mobile.generated.URL;
import org.glob3.mobile.generated.URLTemplateLayer;
import org.glob3.mobile.specific.G3MBuilder_Android;
import org.glob3.mobile.specific.G3MWidget_Android;

import java.util.List;

public class MainActivity extends Activity {

    G3MBuilder_Android _builder;
    G3MWidget_Android _g3mWidget;
    private ConstraintLayout _cl;
    private LocationManager mLocationManager;
    private Location mLocation;
    MarksRenderer _positionRenderer = new MarksRenderer(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Dexter.withActivity(MainActivity.this)
                .withPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            startGlob3();
                        } else {
                            Toast.makeText(MainActivity.this, getText(R.string.ask_for_location), Toast.LENGTH_LONG).show();
                            startGlob3();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<com.karumi.dexter.listener.PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();

        findViewById(R.id.gotolocation).bringToFront();
        findViewById(R.id.gotolocation).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                IThreadUtils tu = _g3mWidget.getG3MContext().getThreadUtils();
                tu.invokeInRendererThread(new GTask() {
                    @Override
                    public void run(G3MContext context) {
                        Log.e("***", "ME EJECUTO");
                        setLocationFromGPS();
                    }
            },true);

            }
        });
    }

    private void startGlob3() {

        _cl = findViewById(R.id.g3m);
        _builder = new G3MBuilder_Android(this);
        _builder.setAtmosphere(true);

        _builder.setInitializationTask(new GInitializationTask() {
            @Override
            public boolean isDone(G3MContext context) {

                setLocationFromGPS();
                return true;
            }

            @Override
            public void run(G3MContext context) {
                    mLocation = getLocation();
            }
        });

        LayerSet layerSet = new LayerSet();
//
        final URLTemplateLayer baseLayer = URLTemplateLayer.newMercator("https://[1234].aerial.maps.cit.api.here.com/maptile/2.1/maptile/newest/satellite.day/{level}/{x}/{y}/256/png8?app_id=DemoAppId01082013GAL&app_code=AJKnXv84fjrb0KIHawS0Tg"
                ,Sector.fullSphere(),false,2,10,TimeInterval.fromDays(30));


        final URLTemplateLayer IGN = URLTemplateLayer.newMercator("http://www.ign.es/wms-inspire/mapa-raster?SERVICE=WMS&LAYERS=mtn_rasterizado&TRANSPARENT=TRUE&FORMAT=image%2Fpng&VERSION=1.3.0&" +
                "REQUEST=GetMap&STYLES=&CRS=EPSG%3A3857&BBOX={west},{south},{east},{north}&WIDTH=256&HEIGHT=256",Sector.fullSphere(),true,2,19,TimeInterval.fromDays(30),true);


        baseLayer.setTitle("Here demo");
        baseLayer.setEnable(true);
        layerSet.addLayer(baseLayer);
        layerSet.addLayer(IGN);



        final URLTemplateLayer catastroLayer = URLTemplateLayer.newMercator("http://ovc.catastro.meh.es/cartografia/INSPIRE/spadgcwms.aspx?LAYERS=CP.CadastralParcel,CP.CadastralZoning,AD.Address,BU.Building,BU.BuildingPart,AU.AdministrativeUnit,AU.AdministrativeBoundary&TRANSPARENT=TRUE&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.3.0&" +
                "REQUEST=GetMap&STYLES=default&SRS=EPSG%3A3857&BBOX={west},{south},{east},{north}&WIDTH=256&HEIGHT=256",Sector.fullSphere(),true,2,19,TimeInterval.fromDays(30),true,  1 ,new LevelTileCondition(16,19));


        catastroLayer.addLayerTouchEventListener(new LayerTouchEventListener() {

            @Override
            public boolean onTerrainTouch(final G3MEventContext context,
                                          final LayerTouchEvent ev) {
                Log.d("****", "EV:"+MercatorUtils.latitudeToMeters(ev.getPosition()._latitude)+","+MercatorUtils.longitudeToMeters(ev.getPosition()._longitude));
                Log.d("****", "URL:"+"https://www1.sedecatastro.gob.es/CYCBienInmueble/OVCListaBienes.aspx?origen=Carto&huso=3857&x="+MercatorUtils.longitudeToMeters( ev.getPosition()._longitude)+ "&y="+MercatorUtils.latitudeToMeters(ev.getPosition()._latitude));
                launchWebViewActivity("https://www1.sedecatastro.gob.es/CYCBienInmueble/OVCListaBienes.aspx?origen=Carto&huso=3857&x="+MercatorUtils.longitudeToMeters( ev.getPosition()._longitude)+ "&y="+MercatorUtils.latitudeToMeters(ev.getPosition()._latitude));
                return true;
            }


            @Override
            public void dispose() {
                // TODO Auto-generated method stub

            }
        });

        layerSet.addLayer(catastroLayer);

        _builder.getPlanetRendererBuilder().setLayerSet(layerSet);
        _builder.addRenderer(_positionRenderer);
        _g3mWidget = _builder.createWidget();
        _cl.addView(_g3mWidget);
    }

    private void setLocationFromGPS() {
        _positionRenderer.removeAllMarks();
        Geodetic3D position = new Geodetic3D(Angle.fromDegrees(40.4146500),Angle.fromDegrees(-3.7004000d), 500);


        if(mLocation != null ){
             position = new Geodetic3D(Angle.fromDegrees(mLocation.getLatitude()), Angle.fromDegrees(mLocation.getLongitude()), 500);
        }


        _g3mWidget.setAnimatedCameraPosition(
                position, TimeInterval.fromSeconds(3));

        _positionRenderer.addMark(new Mark(new URL("file:///position.png",false),
              new Geodetic3D(position.asGeodetic2D(),0),
                AltitudeMode.RELATIVE_TO_GROUND));
    }

    public Location getLocation() {

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if(LocationUtil.getLastKnownLocation(MainActivity.this, mLocationManager) == null ){
            // Si no tenemos acceso al GPS en absoluto, devolvemos siempre el KMO.
            return null;
        }else {
            mLocation = LocationUtil.getLastKnownLocation(MainActivity.this, mLocationManager);
        }
        return mLocation;
    }

    public void launchWebViewActivity(String url){
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra("URL", url);
        startActivity(intent);
    }


}
