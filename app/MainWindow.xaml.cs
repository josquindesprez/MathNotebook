using System;
using System.IO;
using System.Windows;
using Microsoft.Web.WebView2.Core;
using MathNotebook.App.Services;

namespace MathNotebook.App;

public partial class MainWindow : Window
{
    private const string VirtualHost = "mathnotebook.local";
    private HostBridge? _bridge;

    public MainWindow()
    {
        InitializeComponent();
        Loaded += MainWindow_Loaded;
        // Senza una dispose esplicita i processi figlio di WebView2
        // (msedgewebview2.exe: rete, GPU, ecc.) possono restare orfani
        // anche a finestra chiusa. Disporre il controllo alla chiusura
        // libera il CoreWebView2Controller e con esso l'intero albero di
        // processi del browser.
        Closed += (_, _) => WebView.Dispose();
    }

    private async void MainWindow_Loaded(object sender, RoutedEventArgs e)
    {
        // Profilo WebView2 dedicato in una cartella locale scrivibile:
        // l'app può essere installata in percorsi read-only (Program Files),
        // mentre il profilo (cache, cookie di sessione locali) deve poter
        // scrivere. Tutto resta sul disco locale, nessuna rete.
        var userDataFolder = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "MathNotebook", "WebView2Profile");

        var environment = await CoreWebView2Environment.CreateAsync(userDataFolder: userDataFolder);
        await WebView.EnsureCoreWebView2Async(environment);

        var settings = WebView.CoreWebView2.Settings;
        settings.AreDefaultContextMenusEnabled = false;
        settings.AreDevToolsEnabled = true; // utile in sviluppo; nessun impatto su privacy/offline
        settings.IsStatusBarEnabled = false;

        var webRoot = Path.Combine(AppContext.BaseDirectory, "web");
        WebView.CoreWebView2.SetVirtualHostNameToFolderMapping(
            VirtualHost, webRoot, CoreWebView2HostResourceAccessKind.Allow);

        _bridge = new HostBridge(this);
        WebView.CoreWebView2.WebMessageReceived += (_, args) =>
        {
            // Il frontend invia sempre una stringa JSON via
            // chrome.webview.postMessage(JSON.stringify(...)) (vedi
            // web/src/bridge/hostBridge.ts), quindi leggiamo il messaggio
            // grezzo come stringa, non come valore già decodificato.
            var requestJson = args.TryGetWebMessageAsString();
            var responseJson = _bridge.Handle(requestJson);
            WebView.CoreWebView2.PostWebMessageAsJson(responseJson);
        };

        if (File.Exists(Path.Combine(webRoot, "index.html")))
        {
            WebView.CoreWebView2.Navigate($"https://{VirtualHost}/index.html");
        }
        else
        {
            WebView.CoreWebView2.NavigateToString(
                "<html><body style='font-family:sans-serif;padding:2rem'>" +
                "<h1>Bundle frontend non trovato</h1>" +
                "<p>Esegui <code>npm run build</code> in <code>web/</code> e ricompila, " +
                "oppure copia manualmente <code>web/dist</code> in <code>web</code> accanto all'eseguibile.</p>" +
                "</body></html>");
        }
    }
}
