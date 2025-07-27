package rjv.mg.myidm.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

import dagger.hilt.android.AndroidEntryPoint;
import rjv.mg.myidm.R;

@AndroidEntryPoint
public class SettingsFragment extends Fragment {
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupSettingsCards(view);
    }
    
    private void setupSettingsCards(View view) {
        // Download Settings
        MaterialCardView downloadSettingsCard = view.findViewById(R.id.download_settings_card);
        downloadSettingsCard.setOnClickListener(v -> openDownloadSettings());
        
        // Browser Settings
        MaterialCardView browserSettingsCard = view.findViewById(R.id.browser_settings_card);
        browserSettingsCard.setOnClickListener(v -> openBrowserSettings());
        
        // Storage Settings
        MaterialCardView storageSettingsCard = view.findViewById(R.id.storage_settings_card);
        storageSettingsCard.setOnClickListener(v -> openStorageSettings());
        
        // Security Settings
        MaterialCardView securitySettingsCard = view.findViewById(R.id.security_settings_card);
        securitySettingsCard.setOnClickListener(v -> openSecuritySettings());
        
        // About Section
        MaterialCardView aboutCard = view.findViewById(R.id.about_card);
        aboutCard.setOnClickListener(v -> openAbout());
        
        // Rate App
        MaterialCardView rateCard = view.findViewById(R.id.rate_card);
        rateCard.setOnClickListener(v -> rateApp());
        
        // Privacy Policy
        MaterialCardView privacyCard = view.findViewById(R.id.privacy_card);
        privacyCard.setOnClickListener(v -> openPrivacyPolicy());
        
        // Terms of Service
        MaterialCardView termsCard = view.findViewById(R.id.terms_card);
        termsCard.setOnClickListener(v -> openTermsOfService());
    }
    
    private void openDownloadSettings() {
        // TODO: Open download settings fragment
        showComingSoonMessage();
    }
    
    private void openBrowserSettings() {
        // TODO: Open browser settings fragment
        showComingSoonMessage();
    }
    
    private void openStorageSettings() {
        // TODO: Open storage settings fragment
        showComingSoonMessage();
    }
    
    private void openSecuritySettings() {
        // TODO: Open security settings fragment
        showComingSoonMessage();
    }
    
    private void openAbout() {
        // TODO: Open about dialog
        showComingSoonMessage();
    }
    
    private void rateApp() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + requireContext().getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            // Fallback to Play Store website
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + requireContext().getPackageName()));
            startActivity(intent);
        }
    }
    
    private void openPrivacyPolicy() {
        // TODO: Open privacy policy
        showComingSoonMessage();
    }
    
    private void openTermsOfService() {
        // TODO: Open terms of service
        showComingSoonMessage();
    }
    
    private void showComingSoonMessage() {
        android.widget.Toast.makeText(requireContext(), R.string.coming_soon, android.widget.Toast.LENGTH_SHORT).show();
    }
} 