using System;
using System.IO;
using System.Text.Json;
using System.Text.Json.Nodes;
using System.Windows;
using Microsoft.Win32;

namespace MathNotebook.App.Services;

/// <summary>
/// Implementa il protocollo JSON richiesta/risposta descritto in
/// ARCHITECTURE.md ("Bridge nativo"). Il frontend invia
/// { id, method, params } via postMessage; qui rispondiamo con
/// { id, result } oppure { id, error }.
/// Nessuna rete: solo dialoghi nativi e file system locale.
/// </summary>
public sealed class HostBridge
{
    private readonly Window _owner;

    public HostBridge(Window owner)
    {
        _owner = owner;
    }

    public string Handle(string requestJson)
    {
        long id = 0;
        try
        {
            var request = JsonNode.Parse(requestJson)!.AsObject();
            id = request["id"]!.GetValue<long>();
            var method = request["method"]!.GetValue<string>();
            var parameters = request["params"] as JsonObject;

            object? result = method switch
            {
                "file.open" => FileOpen(),
                "file.save" => FileSave(parameters),
                "file.saveAs" => FileSaveAs(parameters),
                "window.setTitle" => WindowSetTitle(parameters),
                _ => throw new InvalidOperationException($"Metodo sconosciuto: {method}"),
            };

            return JsonSerializer.Serialize(new { id, result });
        }
        catch (Exception ex)
        {
            return JsonSerializer.Serialize(new { id, error = ex.Message });
        }
    }

    private object FileOpen()
    {
        var dialog = new OpenFileDialog
        {
            Filter = "Math Notebook (*.mathnb)|*.mathnb|Testo (*.txt)|*.txt|Tutti i file (*.*)|*.*",
            CheckFileExists = true,
        };

        bool? ok = dialog.ShowDialog(_owner);
        if (ok != true)
        {
            return new { canceled = true };
        }

        var contents = File.ReadAllText(dialog.FileName);
        return new { canceled = false, path = dialog.FileName, contents };
    }

    private object FileSave(JsonObject? parameters)
    {
        var path = parameters?["path"]?.GetValue<string>();
        var contents = parameters?["contents"]?.GetValue<string>() ?? string.Empty;
        var suggestedName = parameters?["suggestedName"]?.GetValue<string>() ?? "notebook.mathnb";

        if (string.IsNullOrEmpty(path))
        {
            return FileSaveAsCore(suggestedName, contents);
        }

        File.WriteAllText(path, contents);
        return new { canceled = false, path };
    }

    private object FileSaveAs(JsonObject? parameters)
    {
        var contents = parameters?["contents"]?.GetValue<string>() ?? string.Empty;
        var suggestedName = parameters?["suggestedName"]?.GetValue<string>() ?? "notebook.mathnb";
        return FileSaveAsCore(suggestedName, contents);
    }

    private object FileSaveAsCore(string suggestedName, string contents)
    {
        var dialog = new SaveFileDialog
        {
            Filter = "Math Notebook (*.mathnb)|*.mathnb|Tutti i file (*.*)|*.*",
            FileName = suggestedName,
            DefaultExt = ".mathnb",
        };

        bool? ok = dialog.ShowDialog(_owner);
        if (ok != true)
        {
            return new { canceled = true };
        }

        File.WriteAllText(dialog.FileName, contents);
        return new { canceled = false, path = dialog.FileName };
    }

    private object WindowSetTitle(JsonObject? parameters)
    {
        var title = parameters?["title"]?.GetValue<string>() ?? "Math Notebook";
        _owner.Dispatcher.Invoke(() => _owner.Title = title);
        return new { };
    }
}
