import { Sidebar } from '@/components/layout/sidebar';
import { Navbar } from '@/components/layout/navbar';
import { SidebarProvider } from '@/components/layout/sidebar-context';

export default function StudioLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <SidebarProvider>
      <Sidebar />
      <div className="lg:pl-[220px] min-h-screen flex flex-col">
        <Navbar />
        <main className="flex-1 px-4 sm:px-6 py-6 overflow-x-hidden">{children}</main>
      </div>
    </SidebarProvider>
  );
}
