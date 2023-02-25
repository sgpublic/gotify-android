package com.github.gotify.settings

import android.app.ActivityManager
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.preference.*
import com.github.gotify.R
import com.github.gotify.databinding.SettingsActivityBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

internal class SettingsActivity : AppCompatActivity(), OnSharedPreferenceChangeListener {
    private lateinit var binding: SettingsActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        setSupportActionBar(binding.appBarDrawer.toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowCustomEnabled(true)
        }
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return false
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            getString(R.string.setting_key_theme) -> {
                ThemeHelper.setTheme(
                    this,
                    sharedPreferences.getString(key, getString(R.string.theme_default))!!
                )
            }
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                findPreference<SwitchPreferenceCompat>(
                    getString(R.string.setting_key_notification_channels)
                )?.isEnabled = true
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            findPreference<ListPreference>(
                getString(R.string.setting_key_message_layout)
            )?.let { messageLayout ->
                messageLayout.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
                    showRestartDialog()
                    true
                }
            }
            findPreference<SwitchPreferenceCompat>(
                getString(R.string.setting_key_notification_channels)
            )?.let { notificationChannels ->
                notificationChannels.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        return@OnPreferenceChangeListener false
                    }
                    showRestartDialog()
                    true
                }
            }
            findPreference<CheckBoxPreference>(
                getString(R.string.setting_key_exclude_from_recent)
            )?.let { excludeFromRecents ->
                excludeFromRecents.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _, ->
                    showRestartDialog()
                    true
                }
            }
        }

        override fun onDisplayPreferenceDialog(preference: Preference) {
            if (preference is ListPreference) {
                showListPreferenceDialog(preference)
            } else {
                super.onDisplayPreferenceDialog(preference)
            }
        }

        private fun showListPreferenceDialog(preference: ListPreference) {
            val dialogFragment = MaterialListPreference()
            dialogFragment.arguments = Bundle(1).apply { putString("key", preference.key) }
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(
                parentFragmentManager,
                "androidx.preference.PreferenceFragment.DIALOG"
            )
        }

        private fun showRestartDialog() {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.setting_restart_dialog_title)
                .setMessage(R.string.setting_restart_dialog_message)
                .setPositiveButton(getString(R.string.setting_restart_dialog_button1)) { _, _ ->
                    restartApp()
                }
                .setNegativeButton(getString(R.string.setting_restart_dialog_button2), null)
                .show()
        }

        private fun restartApp() {
            requireContext().getSystemService<ActivityManager>()?.appTasks?.forEach { task ->
                task.finishAndRemoveTask()
            }
            val packageManager = requireContext().packageManager
            val packageName = requireContext().packageName
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            val componentName = intent!!.component
            val mainIntent = Intent.makeRestartActivityTask(componentName)
            startActivity(mainIntent)
        }
    }

    class MaterialListPreference : ListPreferenceDialogFragmentCompat() {
        private var mWhichButtonClicked = 0

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            mWhichButtonClicked = DialogInterface.BUTTON_NEGATIVE
            val builder = MaterialAlertDialogBuilder(requireActivity())
                .setTitle(preference.dialogTitle)
                .setPositiveButton(preference.positiveButtonText, this)
                .setNegativeButton(preference.negativeButtonText, this)

            val contentView = context?.let { onCreateDialogView(it) }
            if (contentView != null) {
                onBindDialogView(contentView)
                builder.setView(contentView)
            } else {
                builder.setMessage(preference.dialogMessage)
            }
            onPrepareDialogBuilder(builder)
            return builder.create()
        }

        override fun onClick(dialog: DialogInterface, which: Int) {
            mWhichButtonClicked = which
        }

        override fun onDismiss(dialog: DialogInterface) {
            onDialogClosedWasCalledFromOnDismiss = true
            super.onDismiss(dialog)
        }

        private var onDialogClosedWasCalledFromOnDismiss = false

        override fun onDialogClosed(positiveResult: Boolean) {
            if (onDialogClosedWasCalledFromOnDismiss) {
                onDialogClosedWasCalledFromOnDismiss = false
                super.onDialogClosed(mWhichButtonClicked == DialogInterface.BUTTON_POSITIVE)
            } else {
                super.onDialogClosed(positiveResult)
            }
        }
    }
}
