import 'package:flutter/material.dart';
import '../services/auth_service.dart';
import '../services/biometric_service.dart';
import '../core/theme/app_colors.dart';
import 'dashboard_screen.dart';
import 'login_screen.dart';

enum _GateStatus { checking, passed, failed }

class AuthWrapper extends StatefulWidget {
  const AuthWrapper({super.key});

  @override
  State<AuthWrapper> createState() => _AuthWrapperState();
}

class _AuthWrapperState extends State<AuthWrapper> {
  bool? _isLoggedIn;
  _GateStatus _gateStatus = _GateStatus.checking;

  @override
  void initState() {
    super.initState();
    _init();
  }

  Future<void> _init() async {
    final loggedIn = await AuthService.isLoggedIn();
    if (!mounted) return;
    setState(() => _isLoggedIn = loggedIn);
    if (loggedIn) await _runBiometricGate();
  }

  Future<void> _runBiometricGate() async {
    setState(() => _gateStatus = _GateStatus.checking);
    final enabled = await BiometricService.isEnabled();
    if (!enabled) {
      if (!mounted) return;
      setState(() => _gateStatus = _GateStatus.passed);
      return;
    }
    final success = await BiometricService.authenticate();
    if (!mounted) return;
    setState(
      () => _gateStatus = success ? _GateStatus.passed : _GateStatus.failed,
    );
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoggedIn == null || _gateStatus == _GateStatus.checking) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    if (_isLoggedIn == false) {
      return const LoginScreen();
    }

    if (_gateStatus == _GateStatus.passed) {
      return const DashboardScreen();
    }

    return Scaffold(
      body: Center(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                Icons.fingerprint,
                size: 56,
                color: AppColors.txt(context).withOpacity(0.6),
              ),
              const SizedBox(height: 16),
              Text(
                'Kimlik doğrulama başarısız',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: AppColors.txt(context),
                ),
              ),
              const SizedBox(height: 24),
              ElevatedButton(
                onPressed: _runBiometricGate,
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 24,
                    vertical: 14,
                  ),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
                child: const Text('Tekrar Dene'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
