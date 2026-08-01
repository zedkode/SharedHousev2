param(
    [Parameter(Mandatory = $true)]
    [string] $SourcePath
)

$ErrorActionPreference = "Stop"

$resolvedSource = (Resolve-Path -LiteralPath $SourcePath).Path
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$androidRoot = Join-Path $repositoryRoot "apps/android"
$designDirectory = Join-Path $androidRoot "design/app-icon"
$resourceRoot = Join-Path $androidRoot "app/src/main/res"

New-Item -ItemType Directory -Path $designDirectory -Force | Out-Null
Copy-Item -LiteralPath $resolvedSource -Destination (Join-Path $designDirectory "sharedhouse-icon-generated-source.png") -Force

if (-not ("SharedHouse.IconProcessor" -as [type])) {
    Add-Type -AssemblyName System.Drawing
    Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;

namespace SharedHouse
{
    public static class IconProcessor
    {
        public static void CreateTransparentMaster(string sourcePath, string outputPath)
        {
            using (var source = new Bitmap(sourcePath))
            using (var output = new Bitmap(source.Width, source.Height, PixelFormat.Format32bppArgb))
            {
                using (var graphics = Graphics.FromImage(output))
                {
                    graphics.CompositingMode = CompositingMode.SourceCopy;
                    graphics.DrawImageUnscaled(source, 0, 0);
                }

                var bounds = new Rectangle(0, 0, output.Width, output.Height);
                var data = output.LockBits(bounds, ImageLockMode.ReadWrite, PixelFormat.Format32bppArgb);
                var bytes = Math.Abs(data.Stride) * data.Height;
                var pixels = new byte[bytes];
                System.Runtime.InteropServices.Marshal.Copy(data.Scan0, pixels, 0, bytes);

                for (var y = 0; y < data.Height; y++)
                {
                    for (var x = 0; x < data.Width; x++)
                    {
                        var index = y * data.Stride + x * 4;
                        var b = pixels[index];
                        var g = pixels[index + 1];
                        var r = pixels[index + 2];

                        // The generated source uses a magenta screen color that never
                        // appears in the green/off-white mark. Removing every pixel whose
                        // red and blue components both dominate green creates a clean mask;
                        // the density resize below adds the final anti-aliased edge.
                        if (Math.Min(r, b) > g + 1)
                        {
                            pixels[index] = 0;
                            pixels[index + 1] = 0;
                            pixels[index + 2] = 0;
                            pixels[index + 3] = 0;
                            continue;
                        }

                        pixels[index + 3] = 255;
                    }
                }

                System.Runtime.InteropServices.Marshal.Copy(pixels, 0, data.Scan0, bytes);
                output.UnlockBits(data);
                output.Save(outputPath, ImageFormat.Png);
            }
        }

        public static void Resize(string sourcePath, string outputPath, int size)
        {
            using (var source = new Bitmap(sourcePath))
            using (var output = new Bitmap(size, size, PixelFormat.Format32bppArgb))
            using (var graphics = Graphics.FromImage(output))
            {
                graphics.CompositingMode = CompositingMode.SourceCopy;
                graphics.CompositingQuality = CompositingQuality.HighQuality;
                graphics.InterpolationMode = InterpolationMode.HighQualityBicubic;
                graphics.PixelOffsetMode = PixelOffsetMode.HighQuality;
                graphics.SmoothingMode = SmoothingMode.HighQuality;
                graphics.DrawImage(source, new Rectangle(0, 0, size, size));
                output.Save(outputPath, ImageFormat.Png);
            }
        }

        private static byte Clamp(double value)
        {
            return (byte)Math.Max(0, Math.Min(255, Math.Round(value)));
        }
    }
}
'@
}

$masterPath = Join-Path $designDirectory "sharedhouse-icon-master.png"
[SharedHouse.IconProcessor]::CreateTransparentMaster($resolvedSource, $masterPath)

$densitySizes = [ordered]@{
    "drawable-mdpi" = 108
    "drawable-hdpi" = 162
    "drawable-xhdpi" = 216
    "drawable-xxhdpi" = 324
    "drawable-xxxhdpi" = 432
}

foreach ($entry in $densitySizes.GetEnumerator()) {
    $directory = Join-Path $resourceRoot $entry.Key
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
    $destination = Join-Path $directory "ic_launcher_foreground_art.png"
    [SharedHouse.IconProcessor]::Resize($masterPath, $destination, $entry.Value)
}

Write-Output "Prepared SharedHouse launcher icon assets from $resolvedSource"
