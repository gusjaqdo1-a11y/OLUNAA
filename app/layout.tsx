import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "OLUNAA — Connect beyond chat",
  description: "A fast, social-first messaging experience for the next generation.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body>{children}</body></html>;
}
