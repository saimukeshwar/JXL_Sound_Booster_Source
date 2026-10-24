package com.example.jxlsoundbooster;

import android.app.Activity;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/**
 * Compatibility-first demo for Android 10 head units.
 *
 * Important: Audio session 0 is the global output mix on some Android builds,
 * but OEM firmware may block global effects for ordinary third-party apps.
 * This app detects failures and reports them; it does not claim to affect all
 * audio when the firmware refuses the effect.
 */
public class MainActivity extends Activity {
    private LinearLayout root;
    private TextView status;
    private Equalizer equalizer;
    private BassBoost bassBoost;
    private LoudnessEnhancer loudnessEnhancer;
    private boolean active = false;
    private int maxBoostMb = 600; // +6 dB software gain; still subject to clipping.
    private SeekBar[] bands;
    private short[] bandCenters;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        tryGlobalEffects();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 20, 28, 24);
        root.setBackgroundColor(Color.rgb(18, 20, 24));
        scroll.addView(root);
        setContentView(scroll);

        TextView title = text("JXL SOUND BOOSTER", 28, Color.WHITE);
        title.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(title);

        TextView subtitle = text("Android 10 • Ad-free • No root", 15, Color.LTGRAY);
        root.addView(subtitle);

        status = text("Checking audio-effect support…", 17, Color.YELLOW);
        status.setPadding(0, 18, 0, 18);
        root.addView(status);

        Button retry = button("Retry system audio connection");
        retry.setOnClickListener(v -> {
            releaseEffects();
            tryGlobalEffects();
        });
        root.addView(retry);

        root.addView(text("MASTER ENHANCEMENT", 20, Color.WHITE));
        SeekBar boost = new SeekBar(this);
        boost.setMax(1200); // -6 dB to +6 dB mapped to 0..1200
        boost.setProgress(600);
        root.addView(boost);
        TextView boostLabel = text("Output gain: 0 dB", 16, Color.LTGRAY);
        root.addView(boostLabel);
        boost.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int p, boolean user) {
                float db = (p - 600) / 100.0f;
                boostLabel.setText(String.format(Locale.US, "Output gain: %.1f dB", db));
                if (loudnessEnhancer != null) {
                    try {
                        loudnessEnhancer.setTargetGain((int)(db * 100));
                        active = true;
                        updateStatus("Global output effect accepted by this firmware.");
                    } catch (Exception e) {
                        updateStatus("Gain control unavailable: " + shortError(e));
                    }
                } else {
                    updateStatus("No global gain effect. This control cannot boost other apps on this unit.");
                }
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });

        root.addView(text("BASS BOOST", 20, Color.WHITE));
        SeekBar bass = new SeekBar(this);
        bass.setMax(1000);
        root.addView(bass);
        bass.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int p, boolean user) {
                if (bassBoost != null) {
                    try {
                        bassBoost.setStrength((short)p);
                        bassBoost.setEnabled(p > 0);
                    } catch (Exception e) { updateStatus("Bass boost unavailable: " + shortError(e)); }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });

        root.addView(text("10-BAND EQUALIZER", 20, Color.WHITE));
        TextView hint = text("Adjust gently. Boosting many bands can cause distortion.", 14, Color.LTGRAY);
        root.addView(hint);
        bands = new SeekBar[10];
        String[] names = {"31 Hz", "62 Hz", "125 Hz", "250 Hz", "500 Hz",
                "1 kHz", "2 kHz", "4 kHz", "8 kHz", "16 kHz"};
        for (int i = 0; i < names.length; i++) {
            LinearLayout line = new LinearLayout(this);
            line.setGravity(Gravity.CENTER_VERTICAL);
            TextView label = text(names[i], 15, Color.WHITE);
            label.setWidth(90);
            line.addView(label);
            SeekBar bar = new SeekBar(this);
            bar.setMax(1200); // map to -12..+12 dB in 0.02 dB increments below
            bar.setProgress(600);
            final int index = i;
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar sb, int p, boolean user) {
                    if (equalizer != null && bandCenters != null && index < bandCenters.length) {
                        try {
                            short level = (short)((p - 600) * 2); // -1200..+1200 mB
                            equalizer.setBandLevel(bandCenters[index], level);
                        } catch (Exception e) { updateStatus("Equalizer control unavailable: " + shortError(e)); }
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar sb) {}
                @Override public void onStopTrackingTouch(SeekBar sb) {}
            });
            bands[i] = bar;
            line.addView(bar, new LinearLayout.LayoutParams(0, 52, 1));
            root.addView(line);
        }

        Button flat = button("Reset EQ to flat");
        flat.setOnClickListener(v -> {
            for (SeekBar b : bands) b.setProgress(600);
            if (equalizer != null && bandCenters != null) {
                try {
                    for (short i = 0; i < bandCenters.length; i++) equalizer.setBandLevel(i, (short)0);
                } catch (Exception ignored) {}
            }
        });
        root.addView(flat);

        root.addView(text("Safety: If sound becomes harsh or distorted, lower gain. Software cannot exceed the head unit's physical amplifier capability.", 14, Color.LTGRAY));
    }

    private void tryGlobalEffects() {
        boolean eqOk = false, bassOk = false, gainOk = false;
        StringBuilder result = new StringBuilder();
        // Session 0 is commonly used to request effects on the global output mix,
        // but many OEM builds reject this for non-system apps.
        try {
            equalizer = new Equalizer(0, 0);
            equalizer.setEnabled(true);
            short count = equalizer.getNumberOfBands();
            short[] range = equalizer.getBandLevelRange();
            bandCenters = new short[count];
            for (short i = 0; i < count; i++) {
                bandCenters[i] = i;
                // If device has fewer than 10 bands, unused UI sliders are harmless.
            }
            eqOk = true;
            result.append("EQ accepted");
        } catch (Exception e) {
            if (equalizer != null) { try { equalizer.release(); } catch (Exception ignored) {} }
            equalizer = null;
            result.append("EQ blocked");
        }
        try {
            bassBoost = new BassBoost(0, 0);
            bassBoost.setEnabled(true);
            bassOk = true;
            result.append(" • bass accepted");
        } catch (Exception e) {
            if (bassBoost != null) { try { bassBoost.release(); } catch (Exception ignored) {} }
            bassBoost = null;
            result.append(" • bass blocked");
        }
        try {
            loudnessEnhancer = new LoudnessEnhancer(0);
            loudnessEnhancer.setTargetGain(0);
            loudnessEnhancer.setEnabled(true);
            gainOk = true;
            result.append(" • gain accepted");
        } catch (Exception e) {
            if (loudnessEnhancer != null) { try { loudnessEnhancer.release(); } catch (Exception ignored) {} }
            loudnessEnhancer = null;
            result.append(" • gain blocked");
        }

        if (eqOk || bassOk || gainOk) {
            updateStatus("Some global effects were accepted: " + result +
                    ". Test FM, Bluetooth and other apps separately; support can differ by source.");
        } else {
            updateStatus("This JXL firmware blocked global effects (" + result +
                    "). A normal app cannot boost all sources on this unit without OEM/system support.");
        }
    }

    private void updateStatus(String message) {
        if (status != null) status.setText(message);
    }

    private String shortError(Exception e) {
        String s = e.getClass().getSimpleName();
        return s == null ? "error" : s;
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setPadding(0, 8, 0, 8);
        return t;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        return b;
    }

    private void releaseEffects() {
        if (equalizer != null) { try { equalizer.release(); } catch (Exception ignored) {} equalizer = null; }
        if (bassBoost != null) { try { bassBoost.release(); } catch (Exception ignored) {} bassBoost = null; }
        if (loudnessEnhancer != null) { try { loudnessEnhancer.release(); } catch (Exception ignored) {} loudnessEnhancer = null; }
        active = false;
    }

    @Override protected void onDestroy() {
        releaseEffects();
        super.onDestroy();
    }
}
