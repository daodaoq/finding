import type { LucideIcon } from 'lucide-react';
import {
  AlertTriangle, Ban, Bell, BookOpen, CalendarDays, Camera, ChevronLeft, ChevronRight, CircleCheck, CircleX, Clock, Eye, FileText, Flag, GraduationCap, GripVertical, Handshake, Heart, Home, ImagePlus, Inbox, Info, Lightbulb, LoaderCircle, Lock, Mail, MapPin, Mars, Megaphone, MessageCircle, MessageSquareText, Palette, PenLine, Pin, ReceiptText, RefreshCw, Ruler, Search, Send, Settings, Share2, Sparkles, Sprout, Star, Target, Trash2, UserRound, Users, Venus, VolumeX, X, type LucideProps,
} from 'lucide-react';

export type AppIconName = 'alert' | 'ban' | 'bell' | 'book' | 'calendar' | 'camera' | 'check' | 'clock' | 'close' | 'eye' | 'file' | 'flag' | 'grad' | 'grip' | 'handshake' | 'heart' | 'home' | 'image' | 'inbox' | 'info' | 'left' | 'lightbulb' | 'loader' | 'location' | 'lock' | 'mail' | 'mars' | 'megaphone' | 'message' | 'mute' | 'palette' | 'pen' | 'pin' | 'receipt' | 'refresh' | 'right' | 'ruler' | 'search' | 'send' | 'settings' | 'share' | 'sparkles' | 'sprout' | 'star' | 'target' | 'thought' | 'trash' | 'user' | 'users' | 'venus' | 'x';

const icons: Record<AppIconName, LucideIcon> = {
  alert: AlertTriangle, ban: Ban, bell: Bell, book: BookOpen, calendar: CalendarDays, camera: Camera, check: CircleCheck, clock: Clock, close: CircleX, eye: Eye, file: FileText, flag: Flag, grad: GraduationCap, grip: GripVertical, handshake: Handshake, heart: Heart, home: Home, image: ImagePlus,
  inbox: Inbox, info: Info, left: ChevronLeft, lightbulb: Lightbulb, loader: LoaderCircle, location: MapPin, lock: Lock, mail: Mail, mars: Mars, megaphone: Megaphone, message: MessageCircle, mute: VolumeX, palette: Palette, pen: PenLine, pin: Pin, receipt: ReceiptText, refresh: RefreshCw, right: ChevronRight, ruler: Ruler, search: Search, send: Send, settings: Settings, share: Share2, sparkles: Sparkles, sprout: Sprout, star: Star, target: Target, thought: MessageSquareText, trash: Trash2, user: UserRound, users: Users, venus: Venus, x: X,
};

interface Props extends LucideProps { name: AppIconName; }

export default function AppIcon({ name, size = 20, strokeWidth = 1.8, ...props }: Props) {
  const Icon = icons[name];
  return <Icon size={size} strokeWidth={strokeWidth} aria-hidden="true" {...props} />;
}
