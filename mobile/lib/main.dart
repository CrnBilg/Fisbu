import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'core/theme/app_theme.dart';
import 'screens/auth_wrapper.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final prefs = await SharedPreferences.getInstance();
  final isDark = prefs.getBool('isDarkMode') ?? false;
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
    ),
  );
  runApp(MyApp(initialDarkMode: isDark));
}

class ThemeController extends ValueNotifier<ThemeMode> {
  ThemeController(bool initialDarkMode)
      : super(initialDarkMode ? ThemeMode.dark : ThemeMode.light);

  bool get isDarkMode => value == ThemeMode.dark;

  Future<void> toggleTheme() async {
    value = isDarkMode ? ThemeMode.light : ThemeMode.dark;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('isDarkMode', isDarkMode);
  }
}

class _ThemeControllerScope extends InheritedNotifier<ThemeController> {
  const _ThemeControllerScope({
    required ThemeController controller,
    required super.child,
  }) : super(notifier: controller);
}

class MyApp extends StatefulWidget {
  final bool initialDarkMode;
  const MyApp({super.key, required this.initialDarkMode});

  static ThemeController? of(BuildContext context) =>
      context
          .dependOnInheritedWidgetOfExactType<_ThemeControllerScope>()
          ?.notifier;

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  late final ThemeController _themeController;

  @override
  void initState() {
    super.initState();
    _themeController = ThemeController(widget.initialDarkMode);
  }

  @override
  void dispose() {
    _themeController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return _ThemeControllerScope(
      controller: _themeController,
      child: ValueListenableBuilder<ThemeMode>(
        valueListenable: _themeController,
        builder: (context, themeMode, child) {
          return MaterialApp(
            title: 'FişBu',
            debugShowCheckedModeBanner: false,
            theme: AppTheme.light,
            darkTheme: AppTheme.dark.copyWith(
              brightness: Brightness.dark,
              scaffoldBackgroundColor: const Color(0xFF1A1A2E),
            ),
            themeMode: themeMode,
            home: const AuthWrapper(),
          );
        },
      ),
    );
  }
}
