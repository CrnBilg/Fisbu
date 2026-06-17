import 'package:flutter/material.dart';
import '../services/auth_service.dart';
import 'auth_wrapper.dart';

class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Profil'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            const SizedBox(height: 16),
            const CircleAvatar(
              radius: 50,
              backgroundColor: Colors.deepPurple,
              child: Icon(
                Icons.person,
                size: 50,
                color: Colors.white,
              ),
            ),
            const SizedBox(height: 16),
            const Text(
              'Kullanıcı Adı',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 4),
            const Text(
              'kullanici@email.com',
              style: TextStyle(color: Colors.grey),
            ),
            const SizedBox(height: 32),

            // Ayarlar listesi
            ListTile(
              leading: const Icon(Icons.category_outlined),
              title: const Text('Kategorilerim'),
              trailing: const Icon(Icons.chevron_right),
              onTap: () {
                // TODO: Kategori ekranına yönlendirilecek
              },
            ),
            const Divider(height: 1),
            ListTile(
              leading: const Icon(Icons.notifications_outlined),
              title: const Text('Bildirimler'),
              trailing: const Icon(Icons.chevron_right),
              onTap: () {
                // TODO: Bildirim ayarları (Hafta 4)
              },
            ),
            const Divider(height: 1),
            ListTile(
              leading: const Icon(Icons.info_outline),
              title: const Text('Hakkında'),
              trailing: const Icon(Icons.chevron_right),
              onTap: () {
                // TODO: Hakkında ekranı
              },
            ),
            const Divider(height: 1),

            const Spacer(),

            // Çıkış butonu
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
               onPressed: () async {
                  await AuthService.logout();
                  if (!context.mounted) return;
                  Navigator.pushAndRemoveUntil(
                    context,
                    MaterialPageRoute(
                      builder: (context) => const AuthWrapper(),
                    ),
                    (route) => false,
                  );
                },
                icon: const Icon(Icons.logout, color: Colors.red),
                label: const Text(
                  'Çıkış Yap',
                  style: TextStyle(color: Colors.red),
                ),
                style: OutlinedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  side: const BorderSide(color: Colors.red),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 16),
          ],
        ),
      ),
    );
  }
}