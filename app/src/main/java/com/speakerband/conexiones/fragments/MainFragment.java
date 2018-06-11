package com.speakerband.conexiones.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;

import com.speakerband.R;
import com.speakerband.wifibuddy.DnsSdService;
import com.speakerband.wifibuddy.WiFiDirectHandlerAccessor;
import com.speakerband.wifibuddy.WifiDirectHandler;
import com.speakerband.conexiones.ConnectionActivity;
import com.speakerband.conexiones.adapter.AvailableServicesListViewAdapter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * EL  Fragment principal de la aplicación, que contiene los interruptores
 * y botones para realizar tareas P2P
 */
public class MainFragment extends Fragment
{
    private WiFiDirectHandlerAccessor wifiDirectHandlerAccessor;
    private Switch toggleWifiSwitch;
    private Switch serviceRegistrationSwitch;
    private Switch noPromptServiceRegistrationSwitch;
    private Button discoverServicesButton;
    private RelativeLayout listaDispositivosLayout;
    private ConnectionActivity mainActivity;
    private Toolbar toolbar;
    private static final String TAG = WifiDirectHandler.TAG + "MainFragment";

    private List<DnsSdService> services = new ArrayList<>();
    private AvailableServicesListViewAdapter servicesListAdapter;
    private ListView deviceList;

    private View viewRoot;

    /**
     * Establece el diseño de la interfaz de usuario,
     * inicializa los botones e interruptores y devuelve la vista
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Establece el diseño para la interfaz de usuario
        viewRoot = inflater.inflate(R.layout.fragment_main, container, false);

        // Inicializa Switches
        toggleWifiSwitch = (Switch) viewRoot.findViewById(R.id.toggleWifiSwitch);
        serviceRegistrationSwitch = (Switch) viewRoot.findViewById(R.id.serviceRegistrationSwitch);
        noPromptServiceRegistrationSwitch = (Switch) viewRoot.findViewById(R.id.noPromptServiceRegistrationSwitch);
        listaDispositivosLayout = (RelativeLayout) viewRoot.findViewById(R.id.dispositivos_layout);

        // Inicializa Discover Services Button
        discoverServicesButton = (Button) viewRoot.findViewById(R.id.discoverServicesButton);

        toolbar = (Toolbar) getActivity().findViewById(R.id.mainToolbar);
        toolbar.setTitle("Conectar con Amigos");

        deviceList = (ListView)viewRoot.findViewById(R.id.device_list);


        // Set Toggle escuchador para Wi-Fi Switch
        toggleWifiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            /**
             * Habilitar o deshabilitar Wi-Fi cuando el interruptor está conmutado
             */
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                Log.i(TAG, "\nWi-Fi Switch Toggled");
                if(isChecked) {
                    // Enable Wi-Fi, enable all switches and buttons
                    getHandler().setWifiEnabled(true);
                } else {
                    // Disable Wi-Fi, disable all switches and buttons
                    getHandler().setWifiEnabled(false);
                }
            }
        });

        // Establezca Toggle Listener para Service Registration Switch
        serviceRegistrationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            /**
             * Add or Remove a Local Service when Switch is toggled
             */
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.i(TAG, "\nService Registration Switch Toggled");
                if (isChecked) {
                    // agrega local service
                    if (getHandler().getWifiP2pServiceInfo() == null)
                    {
                        //sourceDeviceNameOtroMovil = getHandler().getThisDevice().deviceName;
                        HashMap<String, String> record = new HashMap<>();
                        record.put("Name", getHandler().getThisDevice().deviceName);
                        record.put("Address", getHandler().getThisDevice().deviceAddress);
                        getHandler().addLocalService("Wi-Fi Buddy", record);
                        noPromptServiceRegistrationSwitch.setEnabled(false);
                    } else {
                        Log.w(TAG, "Service already added");
                    }
                } else {
                    // Elimina local service
                    getHandler().removeService();
                    noPromptServiceRegistrationSwitch.setEnabled(true);
                }
            }
        });

        // Establecer el Oyente Click para el botón Discover Services
        discoverServicesButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Show AvailableServicesFragment when Discover Services Button is clicked
             */
            @Override
            public void onClick(View v) {
                Log.i(TAG, "\nDiscover Services Button Pressed");
                activarBusquedaAmigos();
                listaDispositivosLayout.setVisibility(View.VISIBLE);
            }
        });

        updateToggles();

        return viewRoot;
    }

    private void activarBusquedaAmigos()
    {
        prepareResetButton(viewRoot);
        setServiceList();
        services.clear();
        servicesListAdapter.notifyDataSetChanged();
        Log.d("TIMING", "Discovering started " + (new Date()).getTime());
        registerLocalP2pReceiver();
        getHandler().continuouslyDiscoverServices();
    }

    private void prepareResetButton(View view){
        Button resetButton = (Button)view.findViewById(R.id.reset_button);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetServiceDiscovery();
            }
        });
    }

    /**
     * Sets the service list adapter to display available services
     */
    private void setServiceList() {
        servicesListAdapter = new AvailableServicesListViewAdapter((ConnectionActivity) getActivity(), services);
        deviceList.setAdapter(servicesListAdapter);
    }

    /**
     * Onclick Method for the the reset button to clear the services list
     * and start discovering services again
     */
    private void resetServiceDiscovery(){
        // Clear the list, notify the list adapter, and start discovering services again
        Log.i(TAG, "Restableciendo el  servicio");
        services.clear();
        servicesListAdapter.notifyDataSetChanged();
        getHandler().resetServiceDiscovery();
    }

    private void registerLocalP2pReceiver() {
        Log.i(TAG, "Registro del receptor de difusión P2P local");
        MainFragment.WifiDirectReceiver p2pBroadcastReceiver = new MainFragment.WifiDirectReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiDirectHandler.Action.DNS_SD_SERVICE_AVAILABLE);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(p2pBroadcastReceiver, intentFilter);
        Log.i(TAG, "Receptor de difusión local P2P registrado");
    }


    /**
     * Receptor para recibir intents del WifiDirectHandler para actualizar la IU
     * when Wi-Fi Direct commands are completed
     */
    public class WifiDirectReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the intent sent by WifiDirectHandler when a service is found
            if (intent.getAction().equals(WifiDirectHandler.Action.DNS_SD_SERVICE_AVAILABLE)) {
                String serviceKey = intent.getStringExtra(WifiDirectHandler.SERVICE_MAP_KEY);
                DnsSdService service = getHandler().getDnsSdServiceMap().get(serviceKey);
                Log.d("TIMING", "Service Discovered and Accessed " + (new Date()).getTime());
                // Add the service to the UI and update
                servicesListAdapter.addUnique(service);
                // TODO Capture an intent that indicates the peer list has changed
                // and see if we need to remove anything from our list
            }
        }
    }


    /**
     * Establece la instancia de la actividad principal
     */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainActivity = (ConnectionActivity) getActivity();
    }

    /**
     * Sets the WifiDirectHandler instance when MainFragment is attached to MainActivity
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            wifiDirectHandlerAccessor = ((WiFiDirectHandlerAccessor) getActivity());
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement WiFiDirectHandlerAccessor");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * Shortcut for accessing the wifi handler
     */
    private WifiDirectHandler getHandler() {
        return wifiDirectHandlerAccessor.getWifiHandler();
    }

    private void updateToggles() {
        // Set state of Switches and Buttons on load
        Log.i(TAG, "Updating toggle switches");
        if(getHandler().isWifiEnabled()) {
            toggleWifiSwitch.setChecked(true);
            serviceRegistrationSwitch.setEnabled(true);
            noPromptServiceRegistrationSwitch.setEnabled(true);
            discoverServicesButton.setEnabled(true);
        } else {
            toggleWifiSwitch.setChecked(false);
            serviceRegistrationSwitch.setEnabled(false);
            noPromptServiceRegistrationSwitch.setEnabled(false);
            discoverServicesButton.setEnabled(false);
        }
    }

    public void handleWifiStateChanged() {
        if (toggleWifiSwitch != null) {
            if (getHandler().isWifiEnabled()) {
                serviceRegistrationSwitch.setEnabled(true);
                discoverServicesButton.setEnabled(true);
            } else {
                serviceRegistrationSwitch.setChecked(false);
                serviceRegistrationSwitch.setEnabled(false);
                discoverServicesButton.setEnabled(false);
            }
        }
    }
}
