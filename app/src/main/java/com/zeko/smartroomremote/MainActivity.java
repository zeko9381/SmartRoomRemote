package com.zeko.smartroomremote;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    // V prihodnosti bo določeno v nastavitvah

    // Ločena nit za omrežne procese, ki se izvajajo v rednem intervalu (pridobivanje temperature in vlage...)
    Timer routineTask = new Timer(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializacija objekta za nastavitve
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Inicializacija View elementov
        TextView temp = findViewById(R.id.tempView);
        TextView hum = findViewById(R.id.humView);
        SeekBar light = findViewById(R.id.seekBar);
        Button party = findViewById(R.id.partyButton);
        Button intruder = findViewById(R.id.intruderButton);

        // Čakalna vrsta za omrežne zahtevke
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        // Inicializacija JSON objekta za pošiljanje strežniku
        JSONObject brightness = new JSONObject();

        // Timer metoda, ki se izvede vsake 10 sekund
        routineTask.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Prebere vrednost, ki smo jo nastavili v Settings dejavnosti
                String url = sharedPreferences.getString("url", "");
                // Pošlje zahtevek na strežnik in poda še metode, ki se izvedejo po določenem dogodku
                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Nastavi besedilo TextView-a na vrednost, ki jo je vrnil strežnik
                            temp.setText(response.get("temperature").toString() + "°C");
                            hum.setText(response.get("humidity").toString() + "%");

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        // Izpiše Toast, ko pride do napake v omrežju
                        Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

                // Doda zahtevek v čakalno vrsto
                requestQueue.add(jsonObjectRequest);
            }
        }, 0, 10000);

        light.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                try {
                    // Ko premaknemo slider, prebere progress in ga zapiše v JSON objekt
                    brightness.put("brightness", light.getProgress());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Ko prenehamo s premikanjem slider-ja, pošlje vrednost progress-a strežniku
                String url = sharedPreferences.getString("url", "");
                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, brightness, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Ob odgovoru ne naredi ničesar. Tu bi lahko strežnik vrnil "OK", da se prepričamo, da se je nek proces izvedel normalno
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

                requestQueue.add(jsonObjectRequest);
            }
        });

        intruder.setOnClickListener(new View.OnClickListener() {
            // Posluša na klik uporabnika. Ko uporabnik klikne na gumb, pošlje zahtevek strežniku
            final String url = sharedPreferences.getString("url", "");

            @Override
            public void onClick(View v) {
                JSONObject event = new JSONObject();
                try {
                    event.put("event", "intruder");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, event, null, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
                        //Mogoča rešitev za Flask. Mogoče potrebuje header-je, ki jih Volley privzeto ne posreduje
                        /*{
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        HashMap<String, String> headers = new HashMap<String, String>();
                        headers.put("Content-Type", "application/json; charset=utf-8");
                        return headers;
                    }
                }*/

                requestQueue.add(jsonObjectRequest);

            }
        });

        party.setOnClickListener(new View.OnClickListener() {
            final String url = sharedPreferences.getString("url", "");
            @Override
            public void onClick(View v) {
                JSONObject event = new JSONObject();
                try {
                    event.put("event", "party");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, event, null, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

                requestQueue.add(jsonObjectRequest);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Za odpiranje treh pikic v zgornjem desnem kotu
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Prebere izbor uporabnika in izvede ukaze odvisno od izbora
        int id = item.getItemId();
        if (id == R.id.about) {
            Intent intent = new Intent(this, Settings.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}