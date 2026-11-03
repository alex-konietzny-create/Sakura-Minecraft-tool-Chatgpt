using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Windows;
using System.Windows.Input;
namespace SakuraLive;
public partial class MainWindow : Window
{
 private record Instance(int Pid,int Port,string Label);
 private readonly HttpClient http=new(){Timeout=TimeSpan.FromSeconds(3)};
 private Instance? connected;
 public MainWindow(){InitializeComponent();Welcome.Text="Welcome, "+Environment.MachineName; RefreshInstances();}
 private void Drag(object sender,MouseButtonEventArgs e){if(e.LeftButton==MouseButtonState.Pressed)DragMove();}
 private void Minimize(object sender,RoutedEventArgs e)=>WindowState=WindowState.Minimized;
 private void Close(object sender,RoutedEventArgs e)=>Close();
 private void Refresh(object sender,RoutedEventArgs e)=>RefreshInstances();
 private void RefreshInstances(){
  Instances.Items.Clear();connected=null;
  string dir=Path.Combine(Path.GetTempPath(),"SakuraLive");
  if(!Directory.Exists(dir)){Status.Text="No Sakura Fabric instances detected";return;}
  foreach(string file in Directory.GetFiles(dir,"*.port")){
   if(!int.TryParse(Path.GetFileNameWithoutExtension(file),out int pid)||!int.TryParse(File.ReadAllText(file).Trim(),out int port))continue;
   try{
    var proc=Process.GetProcessById(pid);
    if(!proc.HasExited && (proc.ProcessName.Equals("javaw",StringComparison.OrdinalIgnoreCase)||proc.ProcessName.Equals("java",StringComparison.OrdinalIgnoreCase)))
      Instances.Items.Add(new Instance(pid,port,$"Minecraft PID {pid} | port {port}"));
   }catch(Exception){}
  }
  if(Instances.Items.Count>0)Instances.SelectedIndex=0;
  Status.Text=$"Detected {Instances.Items.Count} Sakura instance(s)";
 }
 private async void Connect(object sender,RoutedEventArgs e){
  if(Instances.SelectedItem is not Instance i){Status.Text="Select Minecraft first";return;}
  try{
   string reply=await http.GetStringAsync($"http://127.0.0.1:{i.Port}/hello");
   if(reply!="SAKURA:"+i.Pid)throw new Exception("Handshake mismatch");
   connected=i;Status.Text=$"Connected to Minecraft PID {i.Pid} ✓";
  }catch(Exception ex){connected=null;Status.Text="Connection failed: "+ex.Message;}
 }
 private static int Value(System.Windows.Controls.TextBox box,int min,int max){
  if(!int.TryParse(box.Text,out int n)||n<min||n>max)throw new ArgumentException($"Expected {min}–{max}");
  return n;
 }
 private async void Apply(object sender,RoutedEventArgs e){
  if(connected is null){Status.Text="Connect to Minecraft first";return;}
  try{
   var settings=new {anchor=AnchorEnabled.IsChecked==true,crystal=CrystalEnabled.IsChecked==true,
    anchorSlot=Value(AnchorSlot,1,9),glowSlot=Value(GlowSlot,1,9),totemSlot=Value(TotemSlot,1,9),
    crystalSlot=Value(CrystalSlot,1,9),delay=Value(Delay,1,20)};
   using var body=new StringContent(JsonSerializer.Serialize(settings),Encoding.UTF8,"application/json");
   using var resp=await http.PostAsync($"http://127.0.0.1:{connected.Port}/config",body);
   resp.EnsureSuccessStatusCode();Status.Text="Settings applied ✓";
  }catch(Exception ex){Status.Text="Apply failed: "+ex.Message;}
 }
}
